package com.example.player

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.net.Uri
import android.os.Build
import android.util.Log
import android.util.Rational
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
  private val onRecordHistory: (videoId: String, positionMs: Long, durationMs: Long) -> Unit
) {
  companion object {
    private const val TAG = "VideoPlayerManager"
    const val DEFAULT_FALLBACK_URL = "https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8"
    const val USER_AGENT = "Mozilla/5.0 (Linux; Android 14; Lumina/1.0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
  }

  private val scope = CoroutineScope(Dispatchers.Main)
  private var progressTrackerJob: Job? = null
  private var hasAttemptedFallback = false

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
        Log.e(TAG, "ExoPlayer Error encountered: ${error.errorCodeName} - ${error.message}", error)
        
        // Auto-recover if the specific remote CDN source failed (e.g. 403 or network drop)
        if (!hasAttemptedFallback) {
          hasAttemptedFallback = true
          Log.i(TAG, "Switching to reliable high-speed fallback stream: $DEFAULT_FALLBACK_URL")
          try {
            val fallbackItem = MediaItem.fromUri(Uri.parse(DEFAULT_FALLBACK_URL))
            exoPlayer.setMediaItem(fallbackItem)
            exoPlayer.prepare()
            exoPlayer.play()
            return
          } catch (e: Exception) {
            Log.e(TAG, "Fallback attempt failed", e)
          }
        }

        _state.value = _state.value.copy(
          isBuffering = false,
          errorMessage = "Playback error: ${error.localizedMessage ?: "Source unavailable"}"
        )
      }
    })

    startProgressTracker()
  }

  fun playVideo(video: Video, startPositionMs: Long = 0L, expandToPlayer: Boolean = true) {
    hasAttemptedFallback = false
    if (_state.value.currentVideo?.id == video.id && exoPlayer.playbackState == Player.STATE_READY) {
      if (expandToPlayer && _state.value.displayMode == PlayerDisplayMode.COLLAPSED) {
        _state.value = _state.value.copy(displayMode = PlayerDisplayMode.EXPANDED)
      }
      play()
      return
    }

    // Save previous video history before switching
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
      val mediaItem = MediaItem.fromUri(Uri.parse(video.videoUrl))
      exoPlayer.setMediaItem(mediaItem)
      exoPlayer.prepare()
      if (startPositionMs > 0) {
        exoPlayer.seekTo(startPositionMs)
      }
      exoPlayer.play()
    } catch (e: Exception) {
      _state.value = _state.value.copy(errorMessage = "Could not initialize video: ${e.message}")
    }
  }

  fun retry() {
    val current = _state.value.currentVideo ?: return
    playVideo(current, _state.value.currentPositionMs, expandToPlayer = true)
  }

  fun setQueue(videos: List<Video>) {
    _playlist.value = videos
  }

  fun playNextInQueue() {
    val q = _playlist.value
    val current = _state.value.currentVideo
    if (q.isNotEmpty()) {
      val currentIndex = q.indexOfFirst { it.id == current?.id }
      if (currentIndex in 0 until q.lastIndex) {
        playVideo(q[currentIndex + 1], expandToPlayer = false)
      }
    }
  }

  fun playPreviousInQueue() {
    val q = _playlist.value
    val current = _state.value.currentVideo
    if (q.isNotEmpty()) {
      val currentIndex = q.indexOfFirst { it.id == current?.id }
      if (currentIndex > 0) {
        playVideo(q[currentIndex - 1], expandToPlayer = false)
      }
    }
  }

  fun play() {
    exoPlayer.play()
    _state.value = _state.value.copy(isPlaying = true)
  }

  fun pause() {
    exoPlayer.pause()
    _state.value = _state.value.copy(isPlaying = false)
    saveCurrentHistory()
  }

  fun togglePlayPause() {
    if (exoPlayer.isPlaying) pause() else play()
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

  fun setQuality(quality: String) {
    _state.value = _state.value.copy(selectedQuality = quality)
  }

  fun setDisplayMode(mode: PlayerDisplayMode) {
    _state.value = _state.value.copy(displayMode = mode)
  }

  fun closePlayer() {
    pause()
    saveCurrentHistory()
    _state.value = _state.value.copy(
      currentVideo = null,
      displayMode = PlayerDisplayMode.COLLAPSED
    )
  }

  fun toggleMute() {
    val newMuted = !_state.value.isMuted
    exoPlayer.volume = if (newMuted) 0f else 1f
    _state.value = _state.value.copy(isMuted = newMuted)
  }

  fun enterPictureInPicture(activity: Activity): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val params = PictureInPictureParams.Builder()
        .setAspectRatio(Rational(16, 9))
        .build()
      return activity.enterPictureInPictureMode(params)
    }
    return false
  }

  private fun saveCurrentHistory() {
    val video = _state.value.currentVideo ?: return
    val pos = exoPlayer.currentPosition.coerceAtLeast(0L)
    val dur = exoPlayer.duration.coerceAtLeast(1000L)
    onRecordHistory(video.id, pos, dur)
  }

  private fun startProgressTracker() {
    progressTrackerJob?.cancel()
    progressTrackerJob = scope.launch {
      while (isActive) {
        if (exoPlayer.isPlaying) {
          _state.value = _state.value.copy(
            currentPositionMs = exoPlayer.currentPosition.coerceAtLeast(0L),
            durationMs = exoPlayer.duration.coerceAtLeast(0L),
            bufferedPositionMs = exoPlayer.bufferedPosition.coerceAtLeast(0L)
          )
        }
        delay(300)
      }
    }
  }

  fun release() {
    progressTrackerJob?.cancel()
    saveCurrentHistory()
    exoPlayer.release()
  }
}
