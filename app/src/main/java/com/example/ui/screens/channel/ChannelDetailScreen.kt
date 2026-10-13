package com.example.ui.screens.channel

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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.data.model.NotificationLevel
import com.example.data.model.Video
import com.example.data.repository.VideoRepository
import com.example.ui.components.VideoItemCard
import com.example.ui.glass.GlassCard
import com.example.ui.glass.liquidGlassSurface
import com.example.ui.theme.LuminaCyan
import com.example.ui.theme.LuminaViolet

@Composable
fun ChannelDetailScreen(
  channelId: String,
  repository: VideoRepository,
  onPlayVideo: (Video) -> Unit,
  onBack: () -> Unit,
  modifier: Modifier = Modifier
) {
  val channels by repository.channels.collectAsState()
  val allVideos by repository.videos.collectAsState()
  val allShorts by repository.shorts.collectAsState()

  val channel = channels.find { it.id == channelId } ?: channels.firstOrNull() ?: return
  val channelVideos = allVideos.filter { it.channelId == channel.id && !it.isShort }
  val channelShorts = allShorts.filter { it.channelId == channel.id }

  var selectedTab by remember { mutableIntStateOf(0) } // 0: Videos, 1: Shorts, 2: About
  var notificationMenuOpen by remember { mutableStateOf(false) }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(Color(0xFF070A11))
      .testTag("channel_detail_screen")
  ) {
    // Channel Banner with Back button
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(140.dp)
    ) {
      AsyncImage(
        model = channel.bannerUrl,
        contentDescription = "Banner",
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Crop
      )

      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(
            Brush.verticalGradient(
              listOf(Color(0x99000000), Color.Transparent, Color(0xE6070A11))
            )
          )
      )

      IconButton(
        onClick = onBack,
        modifier = Modifier
          .padding(8.dp)
          .align(Alignment.TopStart)
          .size(38.dp)
          .liquidGlassSurface(shape = CircleShape, elevation = 4.dp)
          .testTag("channel_back_btn")
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.ArrowBack,
          contentDescription = "Back",
          tint = Color.White
        )
      }
    }

    // Channel Header
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        AsyncImage(
          model = channel.avatarUrl,
          contentDescription = channel.name,
          modifier = Modifier
            .size(64.dp)
            .clip(CircleShape),
          contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = channel.name,
              style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 17.sp
              )
            )
            if (channel.isVerified) {
              Spacer(modifier = Modifier.width(4.dp))
              Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Verified",
                tint = LuminaCyan,
                modifier = Modifier.size(16.dp)
              )
            }
          }

          Text(
            text = "${channel.handle} • ${channel.formattedSubscribers}",
            style = MaterialTheme.typography.bodySmall.copy(
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          )

          Text(
            text = "${channel.videoCount} videos",
            style = MaterialTheme.typography.labelSmall.copy(
              color = LuminaCyan
            )
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Subscribe Button & Notification bell
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Box(
          modifier = Modifier
            .weight(1f)
            .background(
              brush = if (channel.isSubscribed) {
                Brush.linearGradient(listOf(Color(0x3300F0FF), Color(0x338B5CF6)))
              } else {
                Brush.linearGradient(listOf(LuminaCyan, LuminaViolet))
              },
              shape = RoundedCornerShape(16.dp)
            )
            .clickable { repository.toggleSubscribe(channel.id) }
            .padding(vertical = 10.dp)
            .testTag("channel_subscribe_toggle"),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = if (channel.isSubscribed) "Subscribed" else "Subscribe",
            style = MaterialTheme.typography.titleSmall.copy(
              color = if (channel.isSubscribed) LuminaCyan else Color(0xFF070A11),
              fontWeight = FontWeight.Bold
            )
          )
        }

        if (channel.isSubscribed) {
          Spacer(modifier = Modifier.width(8.dp))
          Box {
            IconButton(
              onClick = { notificationMenuOpen = true },
              modifier = Modifier
                .size(42.dp)
                .liquidGlassSurface(shape = CircleShape, elevation = 4.dp)
            ) {
              Icon(
                imageVector = when (channel.notificationLevel) {
                  NotificationLevel.ALL -> Icons.Default.NotificationsActive
                  NotificationLevel.PERSONALIZED -> Icons.Default.Notifications
                  NotificationLevel.NONE -> Icons.Default.NotificationsNone
                },
                contentDescription = "Notifications",
                tint = LuminaCyan
              )
            }

            DropdownMenu(
              expanded = notificationMenuOpen,
              onDismissRequest = { notificationMenuOpen = false },
              modifier = Modifier.background(Color(0xFF0E1526))
            ) {
              DropdownMenuItem(
                text = { Text("All", color = Color.White) },
                onClick = {
                  repository.setNotificationLevel(channel.id, NotificationLevel.ALL)
                  notificationMenuOpen = false
                }
              )
              DropdownMenuItem(
                text = { Text("Personalized", color = Color.White) },
                onClick = {
                  repository.setNotificationLevel(channel.id, NotificationLevel.PERSONALIZED)
                  notificationMenuOpen = false
                }
              )
              DropdownMenuItem(
                text = { Text("None", color = Color.White) },
                onClick = {
                  repository.setNotificationLevel(channel.id, NotificationLevel.NONE)
                  notificationMenuOpen = false
                }
              )
            }
          }
        }
      }
    }

    // Tabs
    TabRow(
      selectedTabIndex = selectedTab,
      containerColor = Color.Transparent,
      contentColor = LuminaCyan,
      indicator = { tabPositions ->
        TabRowDefaults.SecondaryIndicator(
          modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
          color = LuminaCyan
        )
      }
    ) {
      Tab(
        selected = selectedTab == 0,
        onClick = { selectedTab = 0 },
        text = { Text("Videos (${channelVideos.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
      )
      Tab(
        selected = selectedTab == 1,
        onClick = { selectedTab = 1 },
        text = { Text("Shorts (${channelShorts.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
      )
      Tab(
        selected = selectedTab == 2,
        onClick = { selectedTab = 2 },
        text = { Text("About", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
      )
    }

    // Tab Content
    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 90.dp)
    ) {
      if (selectedTab == 0) {
        items(channelVideos) { video ->
          VideoItemCard(
            video = video,
            onClick = { onPlayVideo(video) },
            onChannelClick = {},
            onSaveToWatchLater = { repository.toggleWatchLater(video.id) },
            onAddToQueue = { repository.addToQueue(video) },
            onShare = {}
          )
        }
      } else if (selectedTab == 1) {
        items(channelShorts) { short ->
          VideoItemCard(
            video = short,
            onClick = { onPlayVideo(short) },
            onChannelClick = {},
            onSaveToWatchLater = { repository.toggleWatchLater(short.id) },
            onAddToQueue = { repository.addToQueue(short) },
            onShare = {}
          )
        }
      } else {
        item {
          Column(modifier = Modifier.padding(16.dp)) {
            Text(
              text = "Description",
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Color.White)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = channel.description,
              style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFFCBD5E1), lineHeight = 20.sp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
              text = "Channel Details",
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Color.White)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text("Handle: ${channel.handle}", color = Color.Gray, fontSize = 13.sp)
            Text("Subscribers: ${channel.formattedSubscribers}", color = Color.Gray, fontSize = 13.sp)
            Text("Total Videos: ${channel.videoCount}", color = Color.Gray, fontSize = 13.sp)
            Text("Verified Partner: ${if (channel.isVerified) "Yes" else "No"}", color = Color.Gray, fontSize = 13.sp)
          }
        }
      }
    }
  }
}
