package com.example.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Subscriptions
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.repository.VideoRepository
import com.example.player.PlayerDisplayMode
import com.example.player.VideoPlayerManager
import com.example.ui.glass.GlassCard
import com.example.ui.glass.LocalUserPreferences
import com.example.ui.glass.liquidGlassSurface
import com.example.ui.screens.channel.ChannelDetailScreen
import com.example.ui.screens.creator.CreatorStudioScreen
import com.example.ui.screens.home.HomeScreen
import com.example.ui.screens.library.LibraryScreen
import com.example.ui.screens.player.PlayerScreen
import com.example.ui.screens.search.SearchScreen
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.screens.shorts.ShortsScreen
import com.example.ui.screens.subscriptions.SubscriptionsScreen
import com.example.ui.theme.LuminaCyan
import com.example.ui.theme.LuminaPink
import com.example.ui.theme.LuminaViolet

enum class AppDestination {
  HOME, SHORTS, SUBSCRIPTIONS, LIBRARY, SEARCH, CHANNEL_DETAIL, CREATOR_STUDIO, SETTINGS
}

@Composable
fun LuminaApp(
  repository: VideoRepository,
  playerManager: VideoPlayerManager
) {
  var currentDestination by remember { mutableStateOf(AppDestination.HOME) }
  var selectedChannelId by remember { mutableStateOf<String?>(null) }
  var targetedShortId by remember { mutableStateOf<String?>(null) }
  var showNotificationsDialog by remember { mutableStateOf(false) }

  val playbackState by playerManager.state.collectAsState()
  val userPrefs by repository.userPreferences.collectAsState()

  val isPlayerExpanded = playbackState.currentVideo != null &&
    (playbackState.displayMode == PlayerDisplayMode.EXPANDED || playbackState.displayMode == PlayerDisplayMode.FULLSCREEN)

  // Back handling
  BackHandler(enabled = isPlayerExpanded || currentDestination != AppDestination.HOME) {
    if (isPlayerExpanded) {
      if (playbackState.displayMode == PlayerDisplayMode.FULLSCREEN) {
        playerManager.setDisplayMode(PlayerDisplayMode.EXPANDED)
      } else {
        playerManager.setDisplayMode(PlayerDisplayMode.COLLAPSED)
      }
    } else if (currentDestination != AppDestination.HOME) {
      currentDestination = AppDestination.HOME
    }
  }

  CompositionLocalProvider(LocalUserPreferences provides userPrefs) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(Color(0xFF070A11))
    ) {
      // SCREEN CONTAINER
      Box(
        modifier = Modifier
          .fillMaxSize()
          .statusBarsPadding()
      ) {
        when (currentDestination) {
          AppDestination.HOME -> {
            HomeScreen(
              repository = repository,
              onPlayVideo = { video ->
                playerManager.playVideo(video, expandToPlayer = true)
              },
              onOpenShorts = { shortId ->
                targetedShortId = shortId
                currentDestination = AppDestination.SHORTS
              },
              onOpenSearch = { currentDestination = AppDestination.SEARCH },
              onOpenChannel = { chId ->
                selectedChannelId = chId
                currentDestination = AppDestination.CHANNEL_DETAIL
              },
              onOpenNotifications = { showNotificationsDialog = true },
              onOpenProfile = { currentDestination = AppDestination.LIBRARY }
            )
          }

          AppDestination.SHORTS -> {
            ShortsScreen(
              repository = repository,
              initialShortId = targetedShortId,
              onOpenChannel = { chId ->
                selectedChannelId = chId
                currentDestination = AppDestination.CHANNEL_DETAIL
              }
            )
          }

          AppDestination.SUBSCRIPTIONS -> {
            SubscriptionsScreen(
              repository = repository,
              onPlayVideo = { video ->
                playerManager.playVideo(video, expandToPlayer = true)
              },
              onOpenChannel = { chId ->
                selectedChannelId = chId
                currentDestination = AppDestination.CHANNEL_DETAIL
              }
            )
          }

          AppDestination.LIBRARY -> {
            LibraryScreen(
              repository = repository,
              onPlayVideo = { video ->
                playerManager.playVideo(video, expandToPlayer = true)
              },
              onOpenSettings = { currentDestination = AppDestination.SETTINGS },
              onOpenCreatorStudio = { currentDestination = AppDestination.CREATOR_STUDIO }
            )
          }

          AppDestination.SEARCH -> {
            SearchScreen(
              repository = repository,
              onPlayVideo = { video ->
                playerManager.playVideo(video, expandToPlayer = true)
              },
              onOpenChannel = { chId ->
                selectedChannelId = chId
                currentDestination = AppDestination.CHANNEL_DETAIL
              },
              onBack = { currentDestination = AppDestination.HOME }
            )
          }

          AppDestination.CHANNEL_DETAIL -> {
            ChannelDetailScreen(
              channelId = selectedChannelId ?: "ch_blender",
              repository = repository,
              onPlayVideo = { video ->
                playerManager.playVideo(video, expandToPlayer = true)
              },
              onBack = { currentDestination = AppDestination.HOME }
            )
          }

          AppDestination.CREATOR_STUDIO -> {
            CreatorStudioScreen(
              repository = repository,
              onBack = { currentDestination = AppDestination.LIBRARY }
            )
          }

          AppDestination.SETTINGS -> {
            SettingsScreen(
              repository = repository,
              onBack = { currentDestination = AppDestination.LIBRARY }
            )
          }
        }
      }

      // FULL / INLINE EXPANDED PLAYER SCREEN OVERLAY
      AnimatedVisibility(
        visible = isPlayerExpanded,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it })
      ) {
        PlayerScreen(
          playerManager = playerManager,
          repository = repository,
          onOpenChannel = { chId ->
            selectedChannelId = chId
            playerManager.setDisplayMode(PlayerDisplayMode.COLLAPSED)
            currentDestination = AppDestination.CHANNEL_DETAIL
          },
          onPlayNextVideo = { nextVid ->
            playerManager.playVideo(nextVid, expandToPlayer = true)
          }
        )
      }

      // COLLAPSED DOCKED MINIPLAYER (Floats above bottom bar when playing and minimized)
      if (playbackState.currentVideo != null && !isPlayerExpanded) {
        val minVideo = playbackState.currentVideo!!
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .align(Alignment.BottomCenter)
            .padding(bottom = 76.dp)
            .padding(horizontal = 12.dp)
            .liquidGlassSurface(shape = RoundedCornerShape(16.dp), elevation = 8.dp)
            .clickable {
              playerManager.setDisplayMode(PlayerDisplayMode.EXPANDED)
            }
            .padding(8.dp)
            .testTag("docked_miniplayer")
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
          ) {
            AsyncImage(
              model = minVideo.thumbnailUrl,
              contentDescription = null,
              modifier = Modifier
                .size(54.dp, 34.dp)
                .clip(RoundedCornerShape(6.dp)),
              contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = minVideo.title,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = Color.White),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
              Text(
                text = minVideo.channelTitle,
                style = MaterialTheme.typography.labelSmall.copy(color = LuminaCyan),
                maxLines = 1
              )
            }

            IconButton(
              onClick = { playerManager.togglePlayPause() },
              modifier = Modifier.size(34.dp)
            ) {
              Icon(
                imageVector = if (playbackState.isPlaying) Icons.Default.Close else Icons.Default.PlayArrow,
                contentDescription = "Toggle play",
                tint = Color.White
              )
            }

            IconButton(
              onClick = { playerManager.closePlayer() },
              modifier = Modifier.size(34.dp)
            ) {
              Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close player",
                tint = Color.Gray
              )
            }
          }
        }
      }

      // FLOATING LIQUID GLASS BOTTOM NAVIGATION BAR
      if (!isPlayerExpanded) {
        Box(
          modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .liquidGlassSurface(shape = RoundedCornerShape(26.dp), elevation = 10.dp)
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag("glass_bottom_navigation")
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
          ) {
            // Home
            NavBarItem(
              icon = if (currentDestination == AppDestination.HOME) Icons.Filled.Home else Icons.Outlined.Home,
              label = "Home",
              isSelected = currentDestination == AppDestination.HOME,
              testTag = "nav_home",
              onClick = { currentDestination = AppDestination.HOME }
            )

            // Shorts
            NavBarItem(
              icon = if (currentDestination == AppDestination.SHORTS) Icons.Filled.Bolt else Icons.Outlined.Bolt,
              label = "Shorts",
              isSelected = currentDestination == AppDestination.SHORTS,
              testTag = "nav_shorts",
              onClick = {
                targetedShortId = null
                currentDestination = AppDestination.SHORTS
              }
            )

            // Center Create / Upload Pill
            Box(
              modifier = Modifier
                .size(46.dp)
                .background(
                  brush = Brush.linearGradient(listOf(LuminaCyan, LuminaViolet)),
                  shape = CircleShape
                )
                .clickable { currentDestination = AppDestination.CREATOR_STUDIO }
                .testTag("nav_create_btn"),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Create",
                tint = Color(0xFF070A11),
                modifier = Modifier.size(28.dp)
              )
            }

            // Subscriptions
            NavBarItem(
              icon = if (currentDestination == AppDestination.SUBSCRIPTIONS) Icons.Filled.Subscriptions else Icons.Outlined.Subscriptions,
              label = "Subs",
              isSelected = currentDestination == AppDestination.SUBSCRIPTIONS,
              testTag = "nav_subs",
              onClick = { currentDestination = AppDestination.SUBSCRIPTIONS }
            )

            // Library
            NavBarItem(
              icon = if (currentDestination == AppDestination.LIBRARY) Icons.Filled.VideoLibrary else Icons.Outlined.VideoLibrary,
              label = "Library",
              isSelected = currentDestination == AppDestination.LIBRARY,
              testTag = "nav_library",
              onClick = { currentDestination = AppDestination.LIBRARY }
            )
          }
        }
      }

      // Notifications Dialog
      if (showNotificationsDialog) {
        AlertDialog(
          onDismissRequest = { showNotificationsDialog = false },
          title = { Text("Activity Notifications", color = Color.White, fontWeight = FontWeight.Bold) },
          text = {
            Column {
              NotificationItem(
                title = "Blender Open Studio uploaded",
                description = "\"Tears of Steel: VFX Sci-Fi Odyssey\" is now streaming in 4K HDR.",
                time = "2 hours ago"
              )
              Spacer(modifier = Modifier.height(10.dp))
              NotificationItem(
                title = "CyberVoxel replied to your comment",
                description = "\"The Liquid Glass reflections are generated via real-time procedural shaders!\"",
                time = "Yesterday"
              )
              Spacer(modifier = Modifier.height(10.dp))
              NotificationItem(
                title = "Chronos Cinema shared a post",
                description = "Behind the scenes 3D breakdown of Sintel's dragon scales.",
                time = "3 days ago"
              )
            }
          },
          confirmButton = {
            TextButton(onClick = { showNotificationsDialog = false }) {
              Text("Close", color = LuminaCyan, fontWeight = FontWeight.Bold)
            }
          },
          containerColor = Color(0xFF0E1526)
        )
      }
    }
  }
}

@Composable
fun NavBarItem(
  icon: ImageVector,
  label: String,
  isSelected: Boolean,
  testTag: String,
  onClick: () -> Unit
) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = Modifier
      .clip(RoundedCornerShape(12.dp))
      .clickable { onClick() }
      .padding(horizontal = 10.dp, vertical = 6.dp)
      .testTag(testTag)
  ) {
    Icon(
      imageVector = icon,
      contentDescription = label,
      tint = if (isSelected) LuminaCyan else Color.White.copy(alpha = 0.7f),
      modifier = Modifier.size(22.dp)
    )

    Spacer(modifier = Modifier.height(2.dp))

    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall.copy(
        color = if (isSelected) LuminaCyan else Color.White.copy(alpha = 0.7f),
        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
        fontSize = 10.sp
      )
    )
  }
}

@Composable
fun NotificationItem(title: String, description: String, time: String) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .background(Color(0x33000000), RoundedCornerShape(8.dp))
      .padding(8.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Text(text = title, color = LuminaCyan, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
      Text(text = time, color = Color.Gray, fontSize = 10.sp)
    }
    Spacer(modifier = Modifier.height(2.dp))
    Text(text = description, color = Color(0xFFE2E8F0), fontSize = 12.sp)
  }
}
