package com.example.ui.screens.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.Channel
import com.example.data.model.Video
import com.example.data.repository.VideoRepository
import com.example.ui.components.VideoItemCard
import com.example.ui.glass.GlassCard
import com.example.ui.glass.liquidGlassSurface
import com.example.ui.theme.LuminaCyan

@Composable
fun SearchScreen(
  repository: VideoRepository,
  onPlayVideo: (Video) -> Unit,
  onOpenChannel: (String) -> Unit,
  onBack: () -> Unit,
  modifier: Modifier = Modifier
) {
  var searchQuery by remember { mutableStateOf("") }
  var filterType by remember { mutableStateOf("All") } // All, Videos, Shorts, Channels

  val allVideos by repository.videos.collectAsState()
  val allShorts by repository.shorts.collectAsState()
  val allChannels by repository.channels.collectAsState()
  val recentQueries by repository.recentSearches.collectAsState(initial = emptyList())

  val trendingSearches = listOf("Tears of Steel 4K", "Liquid Glass UI", "Cyberpunk Tokyo Drift", "Big Buck Bunny HDR", "Blender Open Studio", "Astrophysics Deep Space")

  // Filtered Results
  val queryLower = searchQuery.trim().lowercase()

  val matchingVideos = remember(queryLower, allVideos, allShorts, filterType) {
    if (queryLower.isBlank()) emptyList()
    else {
      val pool = when (filterType) {
        "Shorts" -> allShorts
        "Videos" -> allVideos.filter { !it.isShort }
        else -> allVideos + allShorts
      }
      pool.filter {
        it.title.lowercase().contains(queryLower) ||
          it.description.lowercase().contains(queryLower) ||
          it.category.lowercase().contains(queryLower) ||
          it.channelTitle.lowercase().contains(queryLower) ||
          it.tags.any { tag -> tag.lowercase().contains(queryLower) }
      }
    }
  }

  val matchingChannels = remember(queryLower, allChannels, filterType) {
    if (queryLower.isBlank() || filterType == "Shorts" || filterType == "Videos") emptyList()
    else {
      allChannels.filter {
        it.name.lowercase().contains(queryLower) ||
          it.handle.lowercase().contains(queryLower) ||
          it.description.lowercase().contains(queryLower)
      }
    }
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(Color(0xFF070A11))
      .padding(top = 8.dp)
      .testTag("search_screen_root")
  ) {
    // SEARCH BAR ROW
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 12.dp, vertical = 6.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      IconButton(onClick = onBack, modifier = Modifier.size(38.dp).testTag("search_back_btn")) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.ArrowBack,
          contentDescription = "Back",
          tint = Color.White
        )
      }

      OutlinedTextField(
        value = searchQuery,
        onValueChange = { searchQuery = it },
        placeholder = { Text("Search Lumina...", color = Color.Gray, fontSize = 14.sp) },
        leadingIcon = {
          Icon(Icons.Default.Search, contentDescription = null, tint = LuminaCyan)
        },
        trailingIcon = {
          if (searchQuery.isNotEmpty()) {
            IconButton(onClick = { searchQuery = "" }) {
              Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color.White)
            }
          } else {
            Icon(Icons.Default.Mic, contentDescription = "Voice Search", tint = Color.Gray)
          }
        },
        singleLine = true,
        modifier = Modifier
          .weight(1f)
          .liquidGlassSurface(shape = RoundedCornerShape(22.dp), elevation = 4.dp)
          .testTag("search_text_input"),
        shape = RoundedCornerShape(22.dp),
        colors = OutlinedTextFieldDefaults.colors(
          focusedBorderColor = LuminaCyan,
          unfocusedBorderColor = Color(0x33FFFFFF),
          focusedTextColor = Color.White,
          unfocusedTextColor = Color.White
        )
      )
    }

    // FILTER CHIPS ROW
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState())
        .padding(horizontal = 16.dp, vertical = 6.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      listOf("All", "Videos", "Shorts", "Channels").forEach { f ->
        val isSelected = f == filterType
        Box(
          modifier = Modifier
            .liquidGlassSurface(shape = RoundedCornerShape(16.dp), elevation = 2.dp)
            .background(
              if (isSelected) Color(0x3300F0FF) else Color.Transparent,
              RoundedCornerShape(16.dp)
            )
            .clickable { filterType = f }
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag("search_filter_$f")
        ) {
          Text(
            text = f,
            style = MaterialTheme.typography.labelSmall.copy(
              color = if (isSelected) LuminaCyan else Color.White,
              fontWeight = FontWeight.Bold
            )
          )
        }
      }
    }

    // CONTENT BODY
    if (searchQuery.isBlank()) {
      // Show Recent & Trending searches
      LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp)
      ) {
        if (recentQueries.isNotEmpty()) {
          item {
            Row(
              modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "Recent Searches",
                style = MaterialTheme.typography.titleSmall.copy(color = Color.White, fontWeight = FontWeight.Bold)
              )
              Text(
                text = "Clear all",
                style = MaterialTheme.typography.labelSmall.copy(color = LuminaCyan),
                modifier = Modifier.clickable { repository.clearSearchHistory() }
              )
            }
          }

          items(recentQueries) { query ->
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clickable {
                  searchQuery = query
                  repository.recordSearchQuery(query)
                }
                .padding(vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(Icons.Default.History, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(12.dp))
              Text(text = query, color = Color(0xFFCBD5E1), modifier = Modifier.weight(1f))
            }
          }
        }

        item {
          Spacer(modifier = Modifier.height(16.dp))
          Text(
            text = "Trending on Lumina",
            style = MaterialTheme.typography.titleSmall.copy(color = Color.White, fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(vertical = 8.dp)
          )
        }

        items(trendingSearches) { trend ->
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clickable {
                searchQuery = trend
                repository.recordSearchQuery(trend)
              }
              .padding(vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null, tint = LuminaCyan, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = trend, color = Color.White, fontWeight = FontWeight.Medium)
          }
        }
      }
    } else {
      // SEARCH RESULTS LIST
      LazyColumn(modifier = Modifier.fillMaxSize()) {
        // Channel Results
        if (matchingChannels.isNotEmpty()) {
          item {
            Text(
              text = "Channels",
              style = MaterialTheme.typography.titleSmall.copy(color = Color.White, fontWeight = FontWeight.Bold),
              modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )
          }

          items(matchingChannels) { channel ->
            GlassCard(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
              shape = RoundedCornerShape(14.dp),
              elevation = 4.dp,
              onClick = { onOpenChannel(channel.id) }
            ) {
              Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                AsyncImage(
                  model = channel.avatarUrl,
                  contentDescription = channel.name,
                  modifier = Modifier.size(48.dp).clip(CircleShape),
                  contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Text(text = channel.name, color = Color.White, fontWeight = FontWeight.Bold)
                  Text(text = "${channel.handle} • ${channel.formattedSubscribers}", color = Color.Gray, fontSize = 12.sp)
                }
              }
            }
          }
        }

        // Video Results
        if (matchingVideos.isNotEmpty()) {
          item {
            Text(
              text = "Videos (${matchingVideos.size})",
              style = MaterialTheme.typography.titleSmall.copy(color = Color.White, fontWeight = FontWeight.Bold),
              modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )
          }

          items(matchingVideos) { video ->
            VideoItemCard(
              video = video,
              onClick = {
                repository.recordSearchQuery(searchQuery)
                onPlayVideo(video)
              },
              onChannelClick = onOpenChannel,
              onSaveToWatchLater = { repository.toggleWatchLater(video.id) },
              onAddToQueue = { repository.addToQueue(video) },
              onShare = {}
            )
          }
        }

        if (matchingVideos.isEmpty() && matchingChannels.isEmpty()) {
          item {
            Box(
              modifier = Modifier.fillMaxWidth().padding(48.dp),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = "No results found for \"$searchQuery\"",
                color = Color.Gray,
                style = MaterialTheme.typography.bodyMedium
              )
            }
          }
        }
      }
    }
  }
}
