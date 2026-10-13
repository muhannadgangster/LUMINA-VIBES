package com.example

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.data.repository.VideoRepository
import com.example.player.PlayerDisplayMode
import com.example.player.VideoPlayerManager
import com.example.ui.navigation.LuminaApp
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  private lateinit var repository: VideoRepository
  private lateinit var playerManager: VideoPlayerManager

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    repository = VideoRepository(applicationContext)
    playerManager = VideoPlayerManager(
      context = applicationContext,
      onRecordHistory = { videoId, posMs, durMs ->
        repository.recordWatchHistory(videoId, posMs, durMs)
      }
    )

    setContent {
      MyApplicationTheme(darkTheme = true) {
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = Color(0xFF070A11)
        ) {
          LuminaApp(
            repository = repository,
            playerManager = playerManager
          )
        }
      }
    }
  }

  override fun onPictureInPictureModeChanged(
    isInPictureInPictureMode: Boolean,
    newConfig: Configuration
  ) {
    super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
    if (!isInPictureInPictureMode && playerManager.state.value.displayMode == PlayerDisplayMode.FULLSCREEN) {
      playerManager.setDisplayMode(PlayerDisplayMode.EXPANDED)
    }
  }

  override fun onPause() {
    super.onPause()
    // Pause playback when leaving app unless in PiP
    if (!isInPictureInPictureMode) {
      playerManager.pause()
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    playerManager.release()
  }
}
