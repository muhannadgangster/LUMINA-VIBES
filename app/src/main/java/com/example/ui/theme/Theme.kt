package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
  primary = LuminaCyan,
  onPrimary = Color(0xFF00363D),
  primaryContainer = Color(0xFF004F58),
  onPrimaryContainer = Color(0xFF9CF0FF),
  secondary = LuminaViolet,
  onSecondary = Color(0xFF2C006C),
  secondaryContainer = Color(0xFF491494),
  onSecondaryContainer = Color(0xFFE9DDFF),
  tertiary = LuminaPink,
  onTertiary = Color(0xFF5E001F),
  background = ObsidianBackground,
  onBackground = TextPrimary,
  surface = ObsidianSurface,
  onSurface = TextPrimary,
  surfaceVariant = ObsidianSurfaceVariant,
  onSurfaceVariant = TextSecondary,
  outline = BorderGlass,
  outlineVariant = BorderSubtle
)

private val LightColorScheme = lightColorScheme(
  primary = LuminaCyanDark,
  onPrimary = Color.White,
  primaryContainer = Color(0xFFC7F4FF),
  onPrimaryContainer = Color(0xFF001F24),
  secondary = LuminaVioletDark,
  onSecondary = Color.White,
  secondaryContainer = Color(0xFFE9DDFF),
  onSecondaryContainer = Color(0xFF24005B),
  tertiary = LuminaPink,
  onTertiary = Color.White,
  background = LightBackground,
  onBackground = LightTextPrimary,
  surface = LightSurface,
  onSurface = LightTextPrimary,
  surfaceVariant = LightSurfaceVariant,
  onSurfaceVariant = LightTextSecondary,
  outline = Color(0xFFCBD5E1),
  outlineVariant = Color(0xFFE2E8F0)
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Default to rich cinematic dark theme for video streaming
  content: @Composable () -> Unit
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content
  )
}
