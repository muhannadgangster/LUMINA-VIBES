package com.example.ui.screens.subscriptions

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.Video
import com.example.data.repository.VideoRepository
import com.example.ui.components.VideoItemCard
import com.example.ui.glass.liquidGlassSurface
import com.example.ui.theme.LuminaCyan
import com.example.ui.theme.LuminaViolet

@Composable
fun SubscriptionsScreen(
  repository: VideoRepository,
  onPlayVideo: (Video) -> Unit,
  onOpenChannel: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  val channels by repository.channels.collectAsState()
  val allVideos by repository.videos.collectAsState()
  var selectedFilter by remember { mutableStateOf("All") }

  val subscribedChannels = channels.filter { it.isSubscribed }
  val subscriptionFeedVideos = remember(allVideos, subscribedChannels, selectedFilter) {
    val subChannelIds = subscribedChannels.map { it.id }.toSet()
    allVideos.filter { subChannelIds.contains(it.channelId) || it.isSubscribed }
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(Color(0xFF070A11))
      .padding(top = 8.dp)
      .testTag("subscriptions_screen_root")
  ) {
    // Header
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 6.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Text(
        text = "Subscriptions",
        style = MaterialTheme.typography.titleLarge.copy(
          fontWeight = FontWeight.Bold,
          color = Color.White
        )
      )

      Text(
        text = "${subscribedChannels.size} channels",
        style = MaterialTheme.typography.labelMedium.copy(color = LuminaCyan)
      )
    }

    // Channels Horizontal Rail with Liquid Glass glowing rings
    LazyRow(
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 8.dp),
      horizontalArrangement = Arrangement.spacedBy(14.dp),
      contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp)
    ) {
      items(channels) { channel ->
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          modifier = Modifier
            .clickable { onOpenChannel(channel.id) }
            .testTag("sub_channel_avatar_${channel.id}")
        ) {
          Box(
            modifier = Modifier
              .size(58.dp)
              .border(
                width = 2.dp,
                brush = if (channel.isSubscribed) {
                  Brush.sweepGradient(listOf(LuminaCyan, LuminaViolet, LuminaCyan))
                } else {
                  Brush.linearGradient(listOf(Color(0x33FFFFFF), Color(0x11FFFFFF)))
                },
                shape = CircleShape
              )
              .padding(3.dp)
          ) {
            AsyncImage(
              model = channel.avatarUrl,
              contentDescription = channel.name,
              modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape),
              contentScale = ContentScale.Crop
            )
          }

          Spacer(modifier = Modifier.height(4.dp))

          Text(
            text = channel.name,
            style = MaterialTheme.typography.labelSmall.copy(
              color = Color.White,
              fontWeight = FontWeight.Medium,
              fontSize = 11.sp
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(62.dp)
          )
        }
      }
    }

    // Filter Chips
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState())
        .padding(horizontal = 16.dp, vertical = 6.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      listOf("All", "Today", "Live", "Shorts", "Unwatched").forEach { filter ->
        val isSelected = filter == selectedFilter
        Box(
          modifier = Modifier
            .liquidGlassSurface(shape = RoundedCornerShape(16.dp), elevation = 2.dp)
            .background(
              if (isSelected) Color(0x3300F0FF) else Color.Transparent,
              RoundedCornerShape(16.dp)
            )
            .clickable { selectedFilter = filter }
            .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
          Text(
            text = filter,
            style = MaterialTheme.typography.labelSmall.copy(
              color = if (isSelected) LuminaCyan else Color.White,
              fontWeight = FontWeight.Bold
            )
          )
        }
      }
    }

    // Feed Videos
    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 90.dp)
    ) {
      if (subscriptionFeedVideos.isEmpty()) {
        item {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .padding(48.dp),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = "No recent uploads from subscribed channels.",
              color = Color.Gray,
              style = MaterialTheme.typography.bodyMedium
            )
          }
        }
      } else {
        items(subscriptionFeedVideos) { video ->
          VideoItemCard(
            video = video,
            onClick = { onPlayVideo(video) },
            onChannelClick = onOpenChannel,
            onSaveToWatchLater = { repository.toggleWatchLater(video.id) },
            onAddToQueue = { repository.addToQueue(video) },
            onShare = {}
          )
        }
      }
    }
  }
}
