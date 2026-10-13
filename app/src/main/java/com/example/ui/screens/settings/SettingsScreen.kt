package com.example.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.InvertColors
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ai.GeminiAssistant
import com.example.data.model.GlassMode
import com.example.data.repository.VideoRepository
import com.example.ui.glass.GlassCard
import com.example.ui.glass.liquidGlassSurface
import com.example.ui.theme.LuminaCyan
import com.example.ui.theme.LuminaPink
import com.example.ui.theme.LuminaViolet

@Composable
fun SettingsScreen(
  repository: VideoRepository,
  onBack: () -> Unit,
  modifier: Modifier = Modifier
) {
  val prefs by repository.userPreferences.collectAsState()

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(Color(0xFF070A11))
      .padding(top = 8.dp)
      .testTag("settings_screen_root")
  ) {
    // Header
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 12.dp, vertical = 6.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      IconButton(onClick = onBack, modifier = Modifier.size(38.dp).testTag("settings_back_btn")) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
      }
      Spacer(modifier = Modifier.width(8.dp))
      Text(
        text = "Settings & Preferences",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
      )
    }

    LazyColumn(
      modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
      contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 90.dp)
    ) {
      // SECTION 1: ADAPTIVE LIQUID GLASS INTERFACE
      item {
        SectionTitle(icon = Icons.Default.Layers, title = "Adaptive Liquid Glass Interface")

        GlassCard(
          modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
          shape = RoundedCornerShape(16.dp),
          elevation = 4.dp
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Text("Glass Effect Preset", color = Color.White, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))

            // 3-way toggle: Standard, Subtle Glass, Enhanced Glass
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              listOf(
                GlassMode.STANDARD to "Standard",
                GlassMode.SUBTLE to "Subtle Glass",
                GlassMode.ENHANCED to "Enhanced Glass"
              ).forEach { (mode, label) ->
                val isSelected = prefs.glassMode == mode
                Box(
                  modifier = Modifier
                    .weight(1f)
                    .liquidGlassSurface(shape = RoundedCornerShape(12.dp), elevation = 2.dp)
                    .background(if (isSelected) Color(0x3300F0FF) else Color.Transparent, RoundedCornerShape(12.dp))
                    .clickable { repository.updatePreferences { it.copy(glassMode = mode) } }
                    .padding(vertical = 10.dp)
                    .testTag("glass_mode_$label"),
                  contentAlignment = Alignment.Center
                ) {
                  Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(
                      color = if (isSelected) LuminaCyan else Color.White,
                      fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                      fontSize = 11.sp
                    )
                  )
                }
              }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Intensity Slider
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text("Refraction & Translucency Intensity", color = Color.White, fontSize = 13.sp)
              Text("${(prefs.glassIntensity * 100).toInt()}%", color = LuminaCyan, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }

            Slider(
              value = prefs.glassIntensity,
              onValueChange = { newIntensity ->
                repository.updatePreferences { it.copy(glassIntensity = newIntensity) }
              },
              valueRange = 0.2f..1.0f,
              colors = SliderDefaults.colors(thumbColor = LuminaCyan, activeTrackColor = LuminaCyan),
              modifier = Modifier.testTag("glass_intensity_slider")
            )

            // Motion Effects Toggle
            SettingSwitchRow(
              title = "Dynamic Specular Motion",
              subtitle = "Shimmering highlights responding to interface animation",
              checked = prefs.motionEffectsEnabled,
              onCheckedChange = { checked ->
                repository.updatePreferences { it.copy(motionEffectsEnabled = checked) }
              }
            )

            // Edge Lighting Toggle
            SettingSwitchRow(
              title = "Refraction Edge Lighting",
              subtitle = "Iridescent chromatic borders on translucent panels",
              checked = prefs.edgeLightingEnabled,
              onCheckedChange = { checked ->
                repository.updatePreferences { it.copy(edgeLightingEnabled = checked) }
              }
            )
          }
        }
      }

      // SECTION 2: SMART VIEWING MODES
      item {
        Spacer(modifier = Modifier.height(16.dp))
        SectionTitle(icon = Icons.Default.RemoveRedEye, title = "Smart Viewing Modes")

        GlassCard(
          modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
          shape = RoundedCornerShape(16.dp),
          elevation = 4.dp
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            SettingSwitchRow(
              title = "Focus Mode",
              subtitle = "Dimmers UI distractions and secondary feed clutter during playback",
              checked = prefs.focusModeEnabled,
              onCheckedChange = { checked ->
                repository.updatePreferences { it.copy(focusModeEnabled = checked) }
              }
            )

            SettingSwitchRow(
              title = "Data Saver Mode",
              subtitle = "Limits background video preloading and optimizes cellular data",
              checked = prefs.dataSaverEnabled,
              onCheckedChange = { checked ->
                repository.updatePreferences { it.copy(dataSaverEnabled = checked) }
              }
            )

            SettingSwitchRow(
              title = "Low-Light / OLED Cinema Mode",
              subtitle = "Deep true blacks with reduced blue emission for dark rooms",
              checked = prefs.lowLightModeEnabled,
              onCheckedChange = { checked ->
                repository.updatePreferences { it.copy(lowLightModeEnabled = checked) }
              }
            )

            SettingSwitchRow(
              title = "Reduced Motion Preference",
              subtitle = "Disables non-essential motion transitions and glass shaders",
              checked = prefs.reducedMotion,
              onCheckedChange = { checked ->
                repository.updatePreferences { it.copy(reducedMotion = checked) }
              }
            )
          }
        }
      }

      // SECTION 3: PRIVACY DASHBOARD
      item {
        Spacer(modifier = Modifier.height(16.dp))
        SectionTitle(icon = Icons.Default.Lock, title = "Privacy Dashboard")

        GlassCard(
          modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
          shape = RoundedCornerShape(16.dp),
          elevation = 4.dp
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            SettingSwitchRow(
              title = "Pause Watch History",
              subtitle = "Do not save newly viewed videos to local history",
              checked = prefs.pauseWatchHistory,
              onCheckedChange = { checked ->
                repository.updatePreferences { it.copy(pauseWatchHistory = checked) }
              }
            )

            SettingSwitchRow(
              title = "Pause Search History",
              subtitle = "Do not save queries to search suggestions",
              checked = prefs.pauseSearchHistory,
              onCheckedChange = { checked ->
                repository.updatePreferences { it.copy(pauseSearchHistory = checked) }
              }
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              Box(
                modifier = Modifier
                  .weight(1f)
                  .liquidGlassSurface(shape = RoundedCornerShape(12.dp), elevation = 2.dp)
                  .clickable { repository.clearWatchHistory() }
                  .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
              ) {
                Text("Clear History", color = LuminaPink, fontSize = 12.sp, fontWeight = FontWeight.Bold)
              }

              Box(
                modifier = Modifier
                  .weight(1f)
                  .liquidGlassSurface(shape = RoundedCornerShape(12.dp), elevation = 2.dp)
                  .clickable { repository.clearSearchHistory() }
                  .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
              ) {
                Text("Clear Searches", color = LuminaCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      }

      // SECTION 4: SMART DATA & STORAGE MANAGER
      item {
        Spacer(modifier = Modifier.height(16.dp))
        SectionTitle(icon = Icons.Default.Storage, title = "Smart Data & Storage Manager")

        GlassCard(
          modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
          shape = RoundedCornerShape(16.dp),
          elevation = 4.dp
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column {
                Text("App Media & Cache Usage", color = Color.White, fontWeight = FontWeight.SemiBold)
                Text("Estimated offline cache: 128.4 MB", color = Color.Gray, fontSize = 12.sp)
              }

              Box(
                modifier = Modifier
                  .background(Color(0x33FF3366), RoundedCornerShape(12.dp))
                  .clickable {
                    repository.updatePreferences { it.copy(offlineStorageUsedMb = 0.0) }
                  }
                  .padding(horizontal = 12.dp, vertical = 6.dp)
                  .testTag("clear_cache_btn")
              ) {
                Text("Clear Cache", color = LuminaPink, fontSize = 11.sp, fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      }

      // SECTION 5: AI COMPANION STATUS
      item {
        Spacer(modifier = Modifier.height(16.dp))
        SectionTitle(icon = Icons.Default.AutoAwesome, title = "Lumina AI Companion Status")

        val isConfigured = GeminiAssistant.isConfigured()

        GlassCard(
          modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
          shape = RoundedCornerShape(16.dp),
          elevation = 4.dp
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier = Modifier
                  .size(10.dp)
                  .background(if (isConfigured) Color(0xFF4ADE80) else LuminaCyan, CircleShape)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = if (isConfigured) "Gemini 3.5 Flash Active" else "Smart Semantic Synthesizer Active",
                color = Color.White,
                fontWeight = FontWeight.Bold
              )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
              text = if (isConfigured) {
                "Connected to Google Gemini API via Secure Secrets configuration for advanced study notes and interactive video Q&A."
              } else {
                "Using Lumina's high-performance built-in video synthesis engine for chapter navigation, study notes, and Q&A. You can optionally set GEMINI_API_KEY in the Secrets panel to activate live cloud intelligence."
              },
              color = Color(0xFFCBD5E1),
              fontSize = 12.sp,
              lineHeight = 18.sp
            )
          }
        }
      }
    }
  }
}

@Composable
fun SectionTitle(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier.padding(vertical = 6.dp)
  ) {
    Icon(imageVector = icon, contentDescription = null, tint = LuminaCyan, modifier = Modifier.size(18.dp))
    Spacer(modifier = Modifier.width(8.dp))
    Text(text = title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Color.White))
  }
}

@Composable
fun SettingSwitchRow(
  title: String,
  subtitle: String,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Text(text = title, color = Color.White, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
      Text(text = subtitle, color = Color.Gray, style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp))
    }

    Switch(
      checked = checked,
      onCheckedChange = onCheckedChange,
      colors = SwitchDefaults.colors(
        checkedThumbColor = Color(0xFF070A11),
        checkedTrackColor = LuminaCyan,
        uncheckedThumbColor = Color.White,
        uncheckedTrackColor = Color(0x33FFFFFF)
      )
    )
  }
}
