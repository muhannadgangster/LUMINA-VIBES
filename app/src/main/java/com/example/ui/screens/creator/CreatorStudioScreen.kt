package com.example.ui.screens.creator

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.repository.VideoRepository
import com.example.ui.glass.GlassCard
import com.example.ui.glass.liquidGlassSurface
import com.example.ui.theme.LuminaCyan
import com.example.ui.theme.LuminaPink
import com.example.ui.theme.LuminaViolet

@Composable
fun CreatorStudioScreen(
  repository: VideoRepository,
  onBack: () -> Unit,
  modifier: Modifier = Modifier
) {
  val analytics = remember { repository.getCreatorAnalytics() }
  val allVideos by repository.videos.collectAsState()

  val myUploads = allVideos.filter { it.channelId == "user_channel" || it.id.startsWith("custom_") }

  var showUploadDialog by remember { mutableStateOf(false) }
  var uploadTitle by remember { mutableStateOf("") }
  var uploadDesc by remember { mutableStateOf("") }
  var uploadCategory by remember { mutableStateOf("Tech & Design") }
  var uploadUrl by remember { mutableStateOf("") }
  var uploadIsShort by remember { mutableStateOf(false) }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(Color(0xFF070A11))
      .padding(top = 8.dp)
      .testTag("creator_studio_root")
  ) {
    // Top Bar
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 12.dp, vertical = 6.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack, modifier = Modifier.size(38.dp).testTag("studio_back_btn")) {
          Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "Creator Studio",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
        )
      }

      Box(
        modifier = Modifier
          .background(
            Brush.horizontalGradient(listOf(LuminaCyan, LuminaViolet)),
            RoundedCornerShape(16.dp)
          )
          .clickable { showUploadDialog = true }
          .padding(horizontal = 12.dp, vertical = 8.dp)
          .testTag("upload_video_btn"),
        contentAlignment = Alignment.Center
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.Upload, contentDescription = null, tint = Color(0xFF070A11), modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Upload", color = Color(0xFF070A11), fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
      }
    }

    LazyColumn(
      modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
      contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 90.dp)
    ) {
      // Channel Performance Overview Cards
      item {
        Text(
          text = "Channel Performance (Last 28 Days)",
          style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Color.White),
          modifier = Modifier.padding(vertical = 8.dp)
        )
      }

      item {
        Row(
          modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          StatCard(
            title = "Views",
            value = "184.5K",
            subtext = "↑ +14.2%",
            icon = Icons.Default.Visibility,
            iconTint = LuminaCyan,
            modifier = Modifier.weight(1f)
          )

          StatCard(
            title = "Watch Time",
            value = "12.4K hrs",
            subtext = "↑ +8.7%",
            icon = Icons.Default.Timer,
            iconTint = LuminaViolet,
            modifier = Modifier.weight(1f)
          )
        }
      }

      item {
        Row(
          modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          StatCard(
            title = "Subscribers",
            value = "4,820",
            subtext = "↑ +420 this month",
            icon = Icons.Default.People,
            iconTint = LuminaPink,
            modifier = Modifier.weight(1f)
          )

          StatCard(
            title = "Est. Revenue",
            value = "$1,420.75",
            subtext = "RPM: $7.70",
            icon = Icons.Default.MonetizationOn,
            iconTint = Color(0xFF4ADE80),
            modifier = Modifier.weight(1f)
          )
        }
      }

      // My Published Videos
      item {
        Row(
          modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text(
            text = "Your Published Content (${myUploads.size})",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Color.White)
          )
        }
      }

      if (myUploads.isEmpty()) {
        item {
          Box(
            modifier = Modifier.fillMaxWidth().padding(32.dp),
            contentAlignment = Alignment.Center
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Text("No published videos yet.", color = Color.Gray)
              Spacer(modifier = Modifier.height(8.dp))
              Text("Click 'Upload' to publish your first video or short!", color = LuminaCyan, fontSize = 12.sp)
            }
          }
        }
      } else {
        items(myUploads) { video ->
          GlassCard(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            shape = RoundedCornerShape(12.dp),
            elevation = 2.dp
          ) {
            Row(
              modifier = Modifier.padding(10.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              AsyncImage(
                model = video.thumbnailUrl,
                contentDescription = null,
                modifier = Modifier.size(64.dp, 40.dp).clip(RoundedCornerShape(6.dp)),
                contentScale = ContentScale.Crop
              )

              Spacer(modifier = Modifier.width(10.dp))

              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = video.title,
                  style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                  maxLines = 1
                )
                Text(
                  text = "${video.category} • ${if (video.isShort) "⚡ Short" else "Standard"} • ${video.views} views",
                  style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray)
                )
              }
            }
          }
        }
      }
    }
  }

  // UPLOAD VIDEO DIALOG
  if (showUploadDialog) {
    AlertDialog(
      onDismissRequest = { showUploadDialog = false },
      title = { Text("Publish Video to Lumina", color = Color.White, fontWeight = FontWeight.Bold) },
      text = {
        Column {
          OutlinedTextField(
            value = uploadTitle,
            onValueChange = { uploadTitle = it },
            label = { Text("Video Title") },
            placeholder = { Text("e.g. Next-Gen Liquid Glass Architecture") },
            modifier = Modifier.fillMaxWidth().testTag("upload_input_title"),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = LuminaCyan)
          )

          Spacer(modifier = Modifier.height(8.dp))

          OutlinedTextField(
            value = uploadDesc,
            onValueChange = { uploadDesc = it },
            label = { Text("Description") },
            placeholder = { Text("Describe the video content and tags...") },
            modifier = Modifier.fillMaxWidth().testTag("upload_input_desc"),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = LuminaCyan)
          )

          Spacer(modifier = Modifier.height(8.dp))

          OutlinedTextField(
            value = uploadCategory,
            onValueChange = { uploadCategory = it },
            label = { Text("Category") },
            modifier = Modifier.fillMaxWidth().testTag("upload_input_category"),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = LuminaCyan)
          )

          Spacer(modifier = Modifier.height(8.dp))

          OutlinedTextField(
            value = uploadUrl,
            onValueChange = { uploadUrl = it },
            label = { Text("Video URL (optional)") },
            placeholder = { Text("Defaults to high-speed open stream") },
            modifier = Modifier.fillMaxWidth().testTag("upload_input_url"),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = LuminaCyan)
          )

          Spacer(modifier = Modifier.height(8.dp))

          Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
              checked = uploadIsShort,
              onCheckedChange = { uploadIsShort = it },
              modifier = Modifier.testTag("upload_check_is_short")
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("Publish as a Lumina Short (Vertical Video)", color = Color.White, fontSize = 12.sp)
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (uploadTitle.isNotBlank()) {
              repository.publishVideo(
                title = uploadTitle,
                description = uploadDesc,
                category = uploadCategory,
                videoUrl = uploadUrl,
                isShort = uploadIsShort
              )
              showUploadDialog = false
              uploadTitle = ""
              uploadDesc = ""
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = LuminaCyan),
          modifier = Modifier.testTag("upload_submit_btn")
        ) {
          Text("Publish", color = Color(0xFF070A11), fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { showUploadDialog = false }) {
          Text("Cancel", color = Color.Gray)
        }
      },
      containerColor = Color(0xFF0E1526)
    )
  }
}

@Composable
fun StatCard(
  title: String,
  value: String,
  subtext: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  iconTint: Color,
  modifier: Modifier = Modifier
) {
  GlassCard(
    modifier = modifier,
    shape = RoundedCornerShape(14.dp),
    elevation = 4.dp
  ) {
    Column(modifier = Modifier.padding(12.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(text = title, style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray))
        Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(16.dp))
      }
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = value,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
      )
      Text(
        text = subtext,
        style = MaterialTheme.typography.labelSmall.copy(color = iconTint, fontSize = 10.sp)
      )
    }
  }
}
