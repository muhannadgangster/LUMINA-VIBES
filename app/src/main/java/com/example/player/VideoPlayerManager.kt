package com.example.player

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.example.data.model.Video
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class PlayerDisplayMode {
  COLLAPSED, // mini floating player at bottom
  EXPANDED,  // regular inline player above comments/info
  FULLSCREEN // landscape fullscreen
}

data class PlaybackState(
  val currentVideo: Video? = null,
  val isPlaying: Boolean = false,
  val isBuffering: Boolean = false,
  val currentPositionMs: Long = 0L,
  val durationMs: Long = 0L,
  val bufferedPositionMs: Long = 0L,
  val playbackSpeed: Float = 1.0f,
  val selectedQuality: String = "Auto (1080p)",
  val displayMode: PlayerDisplayMode = PlayerDisplayMode.COLLAPSED,
  val isMuted: Boolean = false,
  val isEnded: Boolean = false,
  val errorMessage: String? = null
)

class VideoPlayerManager(
  private val context: Context,
  private val onRecordHistory: (videoId: String, positionMs: Long, durationMs: Long) -> Unit,
  private val onVideoStarted: ((videoId: String) -> Unit)? = null
) {
  companion object {
    private const val TAG = "VideoPlayerManager"
    const val USER_AGENT = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
  }

  private val scope = CoroutineScope(Dispatchers.Main)
  private var progressTrackerJob: Job? = null
  private var recordedViewForCurrentVideo = false

  private val httpDataSourceFactory = DefaultHttpDataSource.Factory()
    .setUserAgent(USER_AGENT)
    .setConnectTimeoutMs(15000)
    .setReadTimeoutMs(15000)
    .setAllowCrossProtocolRedirects(true)

  private val dataSourceFactory = DefaultDataSource.Factory(context, httpDataSourceFactory)

  val exoPlayer: ExoPlayer = ExoPlayer.Builder(context)
    .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
    .build().apply {
      repeatMode = Player.REPEAT_MODE_OFF
    }

  private val _state = MutableStateFlow(PlaybackState())
  val state: StateFlow<PlaybackState> = _state.asStateFlow()

  private val _playlist = MutableStateFlow<List<Video>>(emptyList())
  val playlist: StateFlow<List<Video>> = _playlist.asStateFlow()

  init {
    exoPlayer.addListener(object : Player.Listener {
      override fun onIsPlayingChanged(isPlaying: Boolean) {
        _state.value = _state.value.copy(isPlaying = isPlaying)
        if (isPlaying && !recordedViewForCurrentVideo) {
          recordedViewForCurrentVideo = true
          _state.value.currentVideo?.let { v ->
            onVideoStarted?.invoke(v.id)
          }
        }
      }

      override fun onPlaybackStateChanged(playbackState: Int) {
        when (playbackState) {
          Player.STATE_BUFFERING -> {
            _state.value = _state.value.copy(isBuffering = true)
          }
          Player.STATE_READY -> {
            val dur = exoPlayer.duration.coerceAtLeast(0L)
            _state.value = _state.value.copy(
              isBuffering = false,
              durationMs = dur,
              isEnded = false,
              errorMessage = null
            )
          }
          Player.STATE_ENDED -> {
            _state.value = _state.value.copy(isEnded = true, isPlaying = false)
            playNextInQueue()
          }
          Player.STATE_IDLE -> {
            _state.value = _state.value.copy(isBuffering = false)
          }
        }
      }

      override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
        Log.e(TAG, "ExoPlayer playback error: ${error.errorCodeName} - ${error.message}", error)
        val userFriendlyMessage = when {
          error.message?.contains("403") == true -> "Access forbidden (HTTP 403). The video stream URL may require authorization or is no longer accessible."
          error.message?.contains("404") == true -> "Video source not found (HTTP 404)."
          else -> error.localizedMessage ?: "Failed to stream video. Please check connection and retry."
        }
        _state.value = _state.value.copy(
          isBuffering = false,
          isPlaying = false,
          errorMessage = userFriendlyMessage
        )
      }
    })

    startProgressTracker()
  }

  fun playVideo(video: Video, startPositionMs: Long = 0L, expandToPlayer: Boolean = true) {
    recordedViewForCurrentVideo = false
    if (_state.value.currentVideo?.id == video.id && exoPlayer.playbackState == Player.STATE_READY && _state.value.errorMessage == null) {
      if (expandToPlayer && _state.value.displayMode == PlayerDisplayMode.COLLAPSED) {
        _state.value = _state.value.copy(displayMode = PlayerDisplayMode.EXPANDED)
      }
      play()
      return
    }

    saveCurrentHistory()

    _state.value = _state.value.copy(
      currentVideo = video,
      displayMode = if (expandToPlayer) PlayerDisplayMode.EXPANDED else _state.value.displayMode,
      errorMessage = null,
      isEnded = false,
      currentPositionMs = startPositionMs,
      durationMs = video.durationSeconds * 1000L
    )

    try {
      if (video.videoUrl.isBlank()) {
        _state.value = _state.value.copy(errorMessage = "Video stream URL is empty")
        return
      }
      val mediaItem = MediaItem.fromUri(Uri.parse(video.videoUrl))
      exoPlayer.setMediaItem(mediaItem)
      exoPlayer.prepare()
      if (startPositionMs > 0) {
        exoPlayer.seekTo(startPositionMs)
      }
      exoPlayer.play()
    } catch (e: Exception) {
      Log.e(TAG, "Error initializing playback", e)
      _state.value = _state.value.copy(errorMessage = "Could not initialize video: ${e.message}")
    }
  }

  fun retry() {
    val current = _state.value.currentVideo ?: return
    playVideo(current, _state.value.currentPositionMs, expandToPlayer = true)
  }

  fun setPlaylist(videos: List<Video>) {
    _playlist.value = videos
  }

  fun playNextInQueue() {
    val list = _playlist.value
    if (list.isEmpty()) return
    val currentIndex = list.indexOfFirst { it.id == _state.value.currentVideo?.id }
    val nextIndex = if (currentIndex != -1 && currentIndex + 1 < list.size) currentIndex + 1 else -1
    if (nextIndex != -1) {
      playVideo(list[nextIndex], expandToPlayer = false)
    }
  }

  fun playPreviousInQueue() {
    val list = _playlist.value
    if (list.isEmpty()) return
    val currentIndex = list.indexOfFirst { it.id == _state.value.currentVideo?.id }
    val prevIndex = if (currentIndex > 0) currentIndex - 1 else -1
    if (prevIndex != -1) {
      playVideo(list[prevIndex], expandToPlayer = false)
    }
  }

  fun togglePlayPause() {
    if (exoPlayer.isPlaying) {
      pause()
    } else {
      play()
    }
  }

  fun play() {
    if (_state.value.errorMessage != null) {
      retry()
    } else {
      exoPlayer.play()
    }
  }

  fun pause() {
    exoPlayer.pause()
  }

  fun seekTo(positionMs: Long) {
    exoPlayer.seekTo(positionMs)
    _state.value = _state.value.copy(currentPositionMs = positionMs)
  }

  fun seekForward(deltaMs: Long = 10000L) {
    val newPos = (exoPlayer.currentPosition + deltaMs).coerceAtMost(exoPlayer.duration)
    seekTo(newPos)
  }

  fun seekBackward(deltaMs: Long = 10000L) {
    val newPos = (exoPlayer.currentPosition - deltaMs).coerceAtLeast(0L)
    seekTo(newPos)
  }

  fun setPlaybackSpeed(speed: Float) {
    exoPlayer.playbackParameters = PlaybackParameters(speed)
    _state.value = _state.value.copy(playbackSpeed = speed)
  }

  fun toggleMute() {
    val shouldMute = !_state.value.isMuted
    exoPlayer.volume = if (shouldMute) 0.0f else 1.0f
    _state.value = _state.value.copy(isMuted = shouldMute)
  }

  fun setDisplayMode(mode: PlayerDisplayMode) {
    _state.value = _state.value.copy(displayMode = mode)
  }

  fun collapsePlayer() {
    _state.value = _state.value.copy(displayMode = PlayerDisplayMode.COLLAPSED)
  }

  fun expandPlayer() {
    _state.value = _state.value.copy(displayMode = PlayerDisplayMode.EXPANDED)
  }

  fun closePlayer() {
    saveCurrentHistory()
    exoPlayer.stop()
    exoPlayer.clearMediaItems()
    _state.value = PlaybackState(displayMode = PlayerDisplayMode.COLLAPSED)
  }

  private fun startProgressTracker() {
    progressTrackerJob?.cancel()
    progressTrackerJob = scope.launch {
      while (isActive) {
        if (exoPlayer.isPlaying) {
          val pos = exoPlayer.currentPosition
          val buffered = exoPlayer.bufferedPosition
          val dur = exoPlayer.duration.coerceAtLeast(0L)
          _state.value = _state.value.copy(
            currentPositionMs = pos,
            bufferedPositionMs = buffered,
            durationMs = dur
          )
        }
        delay(350L)
      }
    }
  }

  private fun saveCurrentHistory() {
    val cur = _state.value.currentVideo ?: return
    val pos = exoPlayer.currentPosition
    val dur = exoPlayer.duration.coerceAtLeast(0L)
    if (dur > 0 && pos > 1000L) {
      onRecordHistory(cur.id, pos, dur)
    }
  }

  fun release() {
    saveCurrentHistory()
    progressTrackerJob?.cancel()
    exoPlayer.release()
  }
}
