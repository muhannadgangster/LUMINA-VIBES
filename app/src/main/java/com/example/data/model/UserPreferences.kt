package com.example.data.model

enum class GlassMode {
  STANDARD, SUBTLE, ENHANCED
}

data class UserPreferences(
  val glassMode: GlassMode = GlassMode.ENHANCED,
  val glassIntensity: Float = 0.85f,
  val motionEffectsEnabled: Boolean = true,
  val edgeLightingEnabled: Boolean = true,
  val focusModeEnabled: Boolean = false,
  val dataSaverEnabled: Boolean = false,
  val lowLightModeEnabled: Boolean = false,
  val reducedMotion: Boolean = false,
  val autoplayNext: Boolean = true,
  val defaultPlaybackSpeed: Float = 1.0f,
  val defaultQuality: String = "Auto (1080p)",
  val pauseWatchHistory: Boolean = false,
  val pauseSearchHistory: Boolean = false,
  val offlineStorageUsedMb: Double = 128.4
)
