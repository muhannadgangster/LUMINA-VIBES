package com.example.ui.screens.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.WatchLater
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.HistoryEntry
import com.example.data.model.OfflineDownload
import com.example.data.model.Video
import com.example.data.model.WatchCollection
import com.example.data.repository.VideoRepository
import com.example.ui.glass.GlassCard
import com.example.ui.glass.liquidGlassSurface
import com.example.ui.theme.LuminaCyan
import com.example.ui.theme.LuminaPink
import com.example.ui.theme.LuminaViolet

@Composable
fun LibraryScreen(
  repository: VideoRepository,
  onPlayVideo: (Video) -> Unit,
  onOpenSettings: () -> Unit,
  onOpenCreatorStudio: () -> Unit,
  modifier: Modifier = Modifier
) {
  val allVideos by repository.videos.collectAsState()
  val watchHistory by repository.watchHistory.collectAsState(initial = emptyList())
  val collections by repository.watchCollections.collectAsState(initial = emptyList())
  val downloads by repository.offlineDownloads.collectAsState(initial = emptyList())
  val watchLaterIds by repository.watchLaterIds.collectAsState()

  var showCreateCollectionDialog by remember { mutableStateOf(false) }
  var newCollectionName by remember { mutableStateOf("") }
  var newCollectionDesc by remember { mutableStateOf("") }

  val historyVideos = remember(watchHistory, allVideos) {
    watchHistory.mapNotNull { entry ->
      allVideos.find { it.id == entry.videoId }?.let { v -> Pair(v, entry) }
    }
  }

  val likedVideos = remember(allVideos) {
    allVideos.filter { it.isLiked }
  }

  val watchLaterVideos = remember(allVideos, watchLaterIds) {
    allVideos.filter { watchLaterIds.contains(it.id) }
  }

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .background(Color(0xFF070A11))
      .padding(horizontal = 16.dp)
      .testTag("library_screen_root"),
    contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp)
  ) {
    // Header Profile Row
    item {
      GlassCard(
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = 16.dp),
        shape = RoundedCornerShape(20.dp),
        elevation = 6.dp
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          AsyncImage(
            model = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150&auto=format&fit=crop&q=80",
            contentDescription = "User Avatar",
            modifier = Modifier.size(54.dp).clip(CircleShape),
            contentScale = ContentScale.Crop
          )

          Spacer(modifier = Modifier.width(14.dp))

          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Aria Nova",
              style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = Color.White
              )
            )
            Text(
              text = "@arianova • Lumina Creator",
              style = MaterialTheme.typography.bodySmall.copy(color = LuminaCyan)
            )
          }

          Box(
            modifier = Modifier
              .background(
                Brush.horizontalGradient(listOf(LuminaCyan, LuminaViolet)),
                RoundedCornerShape(14.dp)
              )
              .clickable { onOpenCreatorStudio() }
              .padding(horizontal = 10.dp, vertical = 6.dp)
              .testTag("library_creator_studio_btn")
          ) {
            Text(
              text = "Studio",
              style = MaterialTheme.typography.labelSmall.copy(
                color = Color(0xFF070A11),
                fontWeight = FontWeight.Bold
              )
            )
          }
        }
      }
    }

    // WATCH HISTORY CAROUSEL
    item {
      Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.History, contentDescription = null, tint = LuminaCyan, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("History", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Color.White))
        }

        if (historyVideos.isNotEmpty()) {
          Text(
            text = "Clear history",
            style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray),
            modifier = Modifier.clickable { repository.clearWatchHistory() }.testTag("clear_history_btn")
          )
        }
      }
    }

    if (historyVideos.isEmpty()) {
      item {
        Text("No watch history yet.", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(vertical = 8.dp))
      }
    } else {
      item {
        LazyRow(
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          modifier = Modifier.padding(vertical = 6.dp)
        ) {
          items(historyVideos) { (video, entry) ->
            val progress = if (entry.durationMs > 0) {
              (entry.playbackPositionMs.toFloat() / entry.durationMs.toFloat()).coerceIn(0f, 1f)
            } else 0f

            GlassCard(
              modifier = Modifier.width(160.dp),
              shape = RoundedCornerShape(12.dp),
              elevation = 4.dp,
              onClick = { onPlayVideo(video) }
            ) {
              Column {
                Box(modifier = Modifier.fillMaxWidth().height(90.dp)) {
                  AsyncImage(
                    model = video.thumbnailUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                  )
                  LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(3.dp).align(Alignment.BottomCenter),
                    color = LuminaCyan,
                    trackColor = Color(0x66000000)
                  )
                }
                Column(modifier = Modifier.padding(6.dp)) {
                  Text(text = video.title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
                  Text(text = video.channelTitle, maxLines = 1, style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray))
                }
              }
            }
          }
        }
      }
    }

    item {
      Spacer(modifier = Modifier.height(16.dp))
    }

    // WATCH LATER SECTION
    item {
      Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(Icons.Default.WatchLater, contentDescription = null, tint = LuminaCyan, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("Watch Later (${watchLaterVideos.size})", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Color.White))
      }
    }

    if (watchLaterVideos.isEmpty()) {
      item {
        Text("No videos in Watch Later.", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(vertical = 4.dp))
      }
    } else {
      items(watchLaterVideos) { video ->
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .liquidGlassSurface(shape = RoundedCornerShape(10.dp), elevation = 2.dp)
            .clickable { onPlayVideo(video) }
            .padding(8.dp),
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
            Text(text = video.title, maxLines = 1, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
            Text(text = video.channelTitle, maxLines = 1, style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray))
          }
          IconButton(onClick = { repository.toggleWatchLater(video.id) }) {
            Icon(Icons.Default.Delete, contentDescription = "Remove", tint = Color.Gray, modifier = Modifier.size(18.dp))
          }
        }
      }
    }

    item {
      Spacer(modifier = Modifier.height(16.dp))
    }

    // PERSONAL WATCH COLLECTIONS
    item {
      Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = null, tint = LuminaCyan, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Personal Watch Collections", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Color.White))
        }

        IconButton(
          onClick = { showCreateCollectionDialog = true },
          modifier = Modifier.size(28.dp).testTag("create_collection_btn")
        ) {
          Icon(Icons.Default.Add, contentDescription = "Add Collection", tint = LuminaCyan)
        }
      }
    }

    items(collections) { col ->
      GlassCard(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = 2.dp
      ) {
        Row(
          modifier = Modifier.padding(12.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Box(
            modifier = Modifier
              .size(42.dp)
              .background(Color(0x3300F0FF), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
          ) {
            Icon(Icons.Default.Bookmark, contentDescription = null, tint = LuminaCyan)
          }

          Spacer(modifier = Modifier.width(12.dp))

          Column(modifier = Modifier.weight(1f)) {
            Text(text = col.name, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Color.White))
            Text(text = col.description, style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray), maxLines = 1)
          }

          Text(text = "${col.videoIds.size} videos", style = MaterialTheme.typography.labelSmall.copy(color = LuminaCyan))
        }
      }
    }

    item {
      Spacer(modifier = Modifier.height(16.dp))
    }

    // OFFLINE DOWNLOADS
    item {
      Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(Icons.Default.Download, contentDescription = null, tint = LuminaCyan, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("Offline Downloads (${downloads.size})", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Color.White))
      }
    }

    if (downloads.isEmpty()) {
      item {
        Text("No downloaded videos.", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(vertical = 4.dp))
      }
    } else {
      items(downloads) { dl ->
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .liquidGlassSurface(shape = RoundedCornerShape(10.dp), elevation = 2.dp)
            .clickable {
              val vid = allVideos.find { it.id == dl.videoId }
              if (vid != null) onPlayVideo(vid)
            }
            .padding(8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          AsyncImage(
            model = dl.thumbnailUrl,
            contentDescription = null,
            modifier = Modifier.size(64.dp, 40.dp).clip(RoundedCornerShape(6.dp)),
            contentScale = ContentScale.Crop
          )
          Spacer(modifier = Modifier.width(10.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(text = dl.title, maxLines = 1, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
            Text(text = "${String.format("%.1f", dl.sizeMb)} MB • Ready offline", style = MaterialTheme.typography.labelSmall.copy(color = LuminaCyan))
          }
          IconButton(onClick = { repository.deleteDownload(dl.videoId) }) {
            Icon(Icons.Default.Delete, contentDescription = "Delete download", tint = Color.Gray, modifier = Modifier.size(18.dp))
          }
        }
      }
    }
  }

  // Create Collection Dialog
  if (showCreateCollectionDialog) {
    AlertDialog(
      onDismissRequest = { showCreateCollectionDialog = false },
      title = { Text("New Watch Collection", color = Color.White) },
      text = {
        Column {
          OutlinedTextField(
            value = newCollectionName,
            onValueChange = { newCollectionName = it },
            label = { Text("Collection Name") },
            modifier = Modifier.fillMaxWidth().testTag("new_collection_name")
          )
          Spacer(modifier = Modifier.height(8.dp))
          OutlinedTextField(
            value = newCollectionDesc,
            onValueChange = { newCollectionDesc = it },
            label = { Text("Description") },
            modifier = Modifier.fillMaxWidth().testTag("new_collection_desc")
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (newCollectionName.isNotBlank()) {
              repository.createCollection(newCollectionName, newCollectionDesc)
              newCollectionName = ""
              newCollectionDesc = ""
              showCreateCollectionDialog = false
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = LuminaCyan)
        ) {
          Text("Create", color = Color(0xFF070A11), fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { showCreateCollectionDialog = false }) {
          Text("Cancel", color = Color.Gray)
        }
      },
      containerColor = Color(0xFF0E1526)
    )
  }
}
