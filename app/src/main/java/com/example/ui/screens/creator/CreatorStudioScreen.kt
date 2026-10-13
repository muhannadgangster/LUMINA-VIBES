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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.rememberCoroutineScope
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
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

@Composable
fun CreatorStudioScreen(
  repository: VideoRepository,
  onBack: () -> Unit,
  modifier: Modifier = Modifier
) {
  val scope = rememberCoroutineScope()
  val user = Firebase.auth.currentUser
  val allVideos by repository.videos.collectAsState()

  val myUploads = remember(allVideos, user) {
    if (user != null) {
      allVideos.filter { it.creatorId == user.uid || it.channelId == user.uid }
    } else {
      emptyList()
    }
  }

  val analytics = remember(myUploads) { repository.getCreatorAnalytics() }

  var showUploadDialog by remember { mutableStateOf(false) }
  var uploadTitle by remember { mutableStateOf("") }
  var uploadDesc by remember { mutableStateOf("") }
  var uploadCategory by remember { mutableStateOf("Tech & Design") }
  var uploadUrl by remember { mutableStateOf("") }
  var uploadThumbnailUrl by remember { mutableStateOf("") }
  var uploadIsShort by remember { mutableStateOf(false) }
  var isPublishing by remember { mutableStateOf(false) }
  var uploadErrorMessage by remember { mutableStateOf<String?>(null) }

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
          .clickable {
            uploadErrorMessage = null
            showUploadDialog = true
          }
          .padding(horizontal = 12.dp, vertical = 8.dp)
          .testTag("upload_video_btn"),
        contentAlignment = Alignment.Center
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.Upload, contentDescription = null, tint = Color(0xFF070A11), modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Upload Video", color = Color(0xFF070A11), fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
      }
    }

    LazyColumn(
      modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
      contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 90.dp)
    ) {
      // Channel Performance Overview Cards (Calculated directly from real database metrics)
      item {
        Text(
          text = "Real-Time Channel Analytics",
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
            title = "Total Views",
            value = "${analytics.totalViews}",
            subtext = "Genuine views",
            icon = Icons.Default.Visibility,
            iconTint = LuminaCyan,
            modifier = Modifier.weight(1f)
          )

          StatCard(
            title = "Watch Time",
            value = "${analytics.totalWatchHours} hrs",
            subtext = "Total playback",
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
            value = "${analytics.subscribersCount}",
            subtext = "Active followers",
            icon = Icons.Default.People,
            iconTint = LuminaPink,
            modifier = Modifier.weight(1f)
          )

          StatCard(
            title = "Published Content",
            value = "${myUploads.size}",
            subtext = "Videos in cloud",
            icon = Icons.Default.VideoLibrary,
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
              Text("Click 'Upload Video' above to publish to Firebase!", color = LuminaCyan, fontSize = 12.sp)
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
                model = video.thumbnailUrl.ifBlank { "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=800&auto=format&fit=crop&q=80" },
                contentDescription = null,
                modifier = Modifier.size(64.dp, 40.dp).clip(RoundedCornerShape(6.dp)),
                contentScale = ContentScale.Crop
              )

              Spacer(modifier = Modifier.width(10.dp))

              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = video.title,
                  style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = Color.White),
                  maxLines = 1
                )
                Text(
                  text = "${video.category} • ${if (video.isShort) "⚡ Short" else "Standard"} • ${video.views} views • ${video.likesCount} likes",
                  style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray)
                )
              }
            }
          }
        }
      }
    }
  }

  // Real Upload Dialog to Firebase Firestore
  if (showUploadDialog) {
    AlertDialog(
      onDismissRequest = {
        if (!isPublishing) showUploadDialog = false
      },
      title = {
        Text("Publish Video to Lumina", color = Color.White, fontWeight = FontWeight.Bold)
      },
      text = {
        Column(modifier = Modifier.fillMaxWidth()) {
          if (uploadErrorMessage != null) {
            Text(
              text = uploadErrorMessage ?: "",
              color = Color(0xFFF87171),
              fontSize = 12.sp,
              modifier = Modifier.padding(bottom = 8.dp)
            )
          }

          OutlinedTextField(
            value = uploadTitle,
            onValueChange = { uploadTitle = it },
            label = { Text("Title *") },
            placeholder = { Text("E.g., Quantum Computing Explained") },
            modifier = Modifier.fillMaxWidth().testTag("upload_input_title"),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = LuminaCyan)
          )

          Spacer(modifier = Modifier.height(8.dp))

          OutlinedTextField(
            value = uploadDesc,
            onValueChange = { uploadDesc = it },
            label = { Text("Description") },
            placeholder = { Text("Brief overview of the video...") },
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
            label = { Text("Video Stream URL (MP4 / HLS) *") },
            placeholder = { Text("https://.../video.mp4") },
            modifier = Modifier.fillMaxWidth().testTag("upload_input_url"),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = LuminaCyan)
          )

          Spacer(modifier = Modifier.height(8.dp))

          OutlinedTextField(
            value = uploadThumbnailUrl,
            onValueChange = { uploadThumbnailUrl = it },
            label = { Text("Thumbnail Image URL (optional)") },
            placeholder = { Text("https://.../cover.jpg") },
            modifier = Modifier.fillMaxWidth().testTag("upload_input_thumb"),
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
            Text("Publish as a Lumina Short (Vertical)", color = Color.White, fontSize = 12.sp)
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (uploadTitle.isBlank()) {
              uploadErrorMessage = "Title is required."
              return@Button
            }
            if (uploadUrl.isBlank()) {
              uploadErrorMessage = "Video stream URL is required."
              return@Button
            }
            isPublishing = true
            uploadErrorMessage = null
            scope.launch {
              val res = repository.publishVideo(
                title = uploadTitle,
                description = uploadDesc,
                category = uploadCategory,
                videoUrl = uploadUrl,
                thumbnailUrl = uploadThumbnailUrl.ifBlank { "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=800&auto=format&fit=crop&q=80" },
                isShort = uploadIsShort
              )
              isPublishing = false
              if (res.isSuccess) {
                showUploadDialog = false
                uploadTitle = ""
                uploadDesc = ""
                uploadUrl = ""
                uploadThumbnailUrl = ""
              } else {
                uploadErrorMessage = res.exceptionOrNull()?.localizedMessage ?: "Failed to publish video"
              }
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = LuminaCyan),
          modifier = Modifier.testTag("upload_submit_btn"),
          enabled = !isPublishing
        ) {
          if (isPublishing) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color(0xFF070A11))
          } else {
            Text("Publish", color = Color(0xFF070A11), fontWeight = FontWeight.Bold)
          }
        }
      },
      dismissButton = {
        TextButton(
          onClick = { showUploadDialog = false },
          enabled = !isPublishing
        ) {
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
