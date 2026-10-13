@file:OptIn(
  androidx.media3.common.util.UnstableApi::class,
  androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.example.ui.screens.player

import android.app.Activity
import android.content.Intent
import kotlin.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Queue
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.example.data.ai.GeminiAssistant
import com.example.data.model.Chapter
import com.example.data.model.Comment
import com.example.data.model.NotificationLevel
import com.example.data.model.Video
import com.example.data.model.VideoBookmark
import com.example.data.repository.VideoRepository
import com.example.player.PlayerDisplayMode
import com.example.player.VideoPlayerManager
import com.example.ui.components.VideoItemCard
import com.example.ui.glass.GlassCard
import com.example.ui.glass.GlassPill
import com.example.ui.glass.liquidGlassSurface
import com.example.ui.theme.LuminaCyan
import com.example.ui.theme.LuminaPink
import com.example.ui.theme.LuminaViolet
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
  playerManager: VideoPlayerManager,
  repository: VideoRepository,
  onOpenChannel: (String) -> Unit,
  onPlayNextVideo: (Video) -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val activity = context as? Activity
  val scope = rememberCoroutineScope()
  val snackbarHostState = remember { SnackbarHostState() }

  val state by playerManager.state.collectAsState()
  val currentVideo = state.currentVideo ?: return
  val allVideos by repository.videos.collectAsState()
  val commentsMap by repository.comments.collectAsState()
  val videoComments = commentsMap[currentVideo.id] ?: emptyList()
  val bookmarks by repository.getBookmarks(currentVideo.id).collectAsState(initial = emptyList())
  val universalQueue by repository.universalQueue.collectAsState()

  var controlsVisible by remember { mutableStateOf(true) }
  var selectedTab by remember { mutableIntStateOf(0) } // 0: Companion, 1: Queue, 2: Comments

  // Auto-hide controls after 4 seconds
  LaunchedEffect(controlsVisible, state.isPlaying) {
    if (controlsVisible && state.isPlaying) {
      delay(4000)
      controlsVisible = false
    }
  }

  val isFullscreen = state.displayMode == PlayerDisplayMode.FULLSCREEN

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(Color(0xFF070A11))
      .testTag("player_screen_root")
  ) {
    Column(modifier = Modifier.fillMaxSize()) {
      // VIDEO PLAYER SURFACE
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .then(
            if (isFullscreen) Modifier.fillMaxSize()
            else Modifier.aspectRatio(16f / 9f)
          )
          .background(Color.Black)
      ) {
        // ExoPlayer Native View
        AndroidView(
          factory = { ctx ->
            PlayerView(ctx).apply {
              player = playerManager.exoPlayer
              useController = false
            }
          },
          modifier = Modifier
            .fillMaxSize()
            .clickable(
              interactionSource = remember { MutableInteractionSource() },
              indication = null
            ) {
              controlsVisible = !controlsVisible
            }
        )

        // Buffering indicator
        if (state.isBuffering) {
          CircularProgressIndicator(
            color = LuminaCyan,
            modifier = Modifier.align(Alignment.Center)
          )
        }

        // Error message & Retry action
        if (state.errorMessage != null) {
          Column(
            modifier = Modifier
              .align(Alignment.Center)
              .background(Color(0xCC0E1526), RoundedCornerShape(14.dp))
              .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Text(
              text = state.errorMessage ?: "Playback Error",
              color = LuminaPink,
              style = MaterialTheme.typography.bodySmall,
              fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(10.dp))
            Box(
              modifier = Modifier
                .background(LuminaCyan, RoundedCornerShape(10.dp))
                .clickable { playerManager.retry() }
                .padding(horizontal = 14.dp, vertical = 6.dp),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = "Retry",
                color = Color(0xFF070A11),
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
              )
            }
          }
        }

        // LIQUID GLASS PLAYER CONTROLS OVERLAY (never blurs video!)
        androidx.compose.animation.AnimatedVisibility(
          visible = controlsVisible,
          enter = fadeIn(),
          exit = fadeOut(),
          modifier = Modifier.fillMaxSize()
        ) {
          Box(
            modifier = Modifier
              .fillMaxSize()
              .background(Color(0x55000000))
          ) {
            // TOP CONTROLS BAR
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .align(Alignment.TopCenter),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              // Collapse / Back Button
              IconButton(
                onClick = {
                  if (isFullscreen) {
                    playerManager.setDisplayMode(PlayerDisplayMode.EXPANDED)
                  } else {
                    playerManager.setDisplayMode(PlayerDisplayMode.COLLAPSED)
                  }
                },
                modifier = Modifier
                  .size(38.dp)
                  .liquidGlassSurface(shape = CircleShape, elevation = 4.dp)
                  .testTag("player_collapse_btn")
              ) {
                Icon(
                  imageVector = Icons.Default.KeyboardArrowDown,
                  contentDescription = "Collapse",
                  tint = Color.White
                )
              }

              // Title (compact)
              Text(
                text = currentVideo.title,
                style = MaterialTheme.typography.titleSmall.copy(
                  color = Color.White,
                  fontWeight = FontWeight.SemiBold
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                  .weight(1f)
                  .padding(horizontal = 8.dp)
              )

              // Right Action Controls: Speed, Quality, PiP, Mute
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                // Playback Speed dropdown
                var speedMenuOpen by remember { mutableStateOf(false) }
                Box {
                  Box(
                    modifier = Modifier
                      .liquidGlassSurface(shape = RoundedCornerShape(12.dp), elevation = 4.dp)
                      .clickable { speedMenuOpen = true }
                      .padding(horizontal = 8.dp, vertical = 4.dp)
                      .testTag("player_speed_btn")
                  ) {
                    Text(
                      text = "${state.playbackSpeed}x",
                      style = MaterialTheme.typography.labelSmall.copy(
                        color = LuminaCyan,
                        fontWeight = FontWeight.Bold
                      )
                    )
                  }

                  DropdownMenu(
                    expanded = speedMenuOpen,
                    onDismissRequest = { speedMenuOpen = false },
                    modifier = Modifier.background(Color(0xFF0E1526))
                  ) {
                    listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { spd ->
                      DropdownMenuItem(
                        text = { Text("${spd}x", color = Color.White) },
                        onClick = {
                          playerManager.setPlaybackSpeed(spd)
                          speedMenuOpen = false
                        }
                      )
                    }
                  }
                }

                // PiP Button
                if (activity != null) {
                  IconButton(
                    onClick = { playerManager.enterPictureInPicture(activity) },
                    modifier = Modifier
                      .size(36.dp)
                      .liquidGlassSurface(shape = CircleShape, elevation = 4.dp)
                      .testTag("player_pip_btn")
                  ) {
                    Icon(
                      imageVector = Icons.Default.PictureInPicture,
                      contentDescription = "Picture in picture",
                      tint = Color.White,
                      modifier = Modifier.size(18.dp)
                    )
                  }
                }

                // Mute Button
                IconButton(
                  onClick = { playerManager.toggleMute() },
                  modifier = Modifier
                    .size(36.dp)
                    .liquidGlassSurface(shape = CircleShape, elevation = 4.dp)
                ) {
                  Icon(
                    imageVector = if (state.isMuted) Icons.AutoMirrored.Filled.VolumeMute else Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = "Mute",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                  )
                }
              }
            }

            // CENTER CONTROLS: 10s Rewind, Play/Pause, 10s Forward
            Row(
              modifier = Modifier.align(Alignment.Center),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(28.dp)
            ) {
              // Rewind 10s
              IconButton(
                onClick = { playerManager.seekBackward(10000L) },
                modifier = Modifier
                  .size(48.dp)
                  .liquidGlassSurface(shape = CircleShape, elevation = 6.dp)
                  .testTag("player_rewind_10s")
              ) {
                Icon(
                  imageVector = Icons.Default.FastRewind,
                  contentDescription = "Rewind 10s",
                  tint = Color.White,
                  modifier = Modifier.size(24.dp)
                )
              }

              // Play / Pause Main Spring Button
              Box(
                modifier = Modifier
                  .size(68.dp)
                  .liquidGlassSurface(shape = CircleShape, elevation = 10.dp)
                  .clickable { playerManager.togglePlayPause() }
                  .testTag("player_play_pause_btn"),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = if (state.isPlaying) Icons.Default.Close else Icons.Default.PlayArrow,
                  contentDescription = if (state.isPlaying) "Pause" else "Play",
                  tint = LuminaCyan,
                  modifier = Modifier.size(38.dp)
                )
              }

              // Fast Forward 10s
              IconButton(
                onClick = { playerManager.seekForward(10000L) },
                modifier = Modifier
                  .size(48.dp)
                  .liquidGlassSurface(shape = CircleShape, elevation = 6.dp)
                  .testTag("player_forward_10s")
              ) {
                Icon(
                  imageVector = Icons.Default.FastForward,
                  contentDescription = "Forward 10s",
                  tint = Color.White,
                  modifier = Modifier.size(24.dp)
                )
              }
            }

            // BOTTOM TIMELINE & SCRUBBER CONTROLS
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp)
                .align(Alignment.BottomCenter)
            ) {
              // Scrubber Slider
              val totalDuration = state.durationMs.coerceAtLeast(1000L).toFloat()
              var sliderPosition by remember { mutableFloatStateOf(0f) }
              var isUserDragging by remember { mutableStateOf(false) }

              val displayPos = if (isUserDragging) sliderPosition else state.currentPositionMs.toFloat()

              Slider(
                value = displayPos.coerceIn(0f, totalDuration),
                onValueChange = {
                  isUserDragging = true
                  sliderPosition = it
                },
                onValueChangeFinished = {
                  playerManager.seekTo(sliderPosition.toLong())
                  isUserDragging = false
                },
                valueRange = 0f..totalDuration,
                colors = SliderDefaults.colors(
                  thumbColor = LuminaCyan,
                  activeTrackColor = LuminaCyan,
                  inactiveTrackColor = Color(0x66FFFFFF)
                ),
                modifier = Modifier.fillMaxWidth().height(24.dp).testTag("player_timeline_slider")
              )

              // Time labels & Fullscreen toggle
              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Text(
                  text = "${formatMs(state.currentPositionMs)} / ${formatMs(state.durationMs)}",
                  style = MaterialTheme.typography.labelSmall.copy(
                    color = Color.White,
                    fontWeight = FontWeight.Medium
                  )
                )

                IconButton(
                  onClick = {
                    if (isFullscreen) {
                      playerManager.setDisplayMode(PlayerDisplayMode.EXPANDED)
                    } else {
                      playerManager.setDisplayMode(PlayerDisplayMode.FULLSCREEN)
                    }
                  },
                  modifier = Modifier.size(32.dp).testTag("player_fullscreen_toggle")
                ) {
                  Icon(
                    imageVector = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                    contentDescription = "Fullscreen",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                  )
                }
              }
            }
          }
        }
      }

      // DETAILS & SOCIAL INTERACTIONS (visible in portrait mode)
      if (!isFullscreen) {
        LazyColumn(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .padding(horizontal = 14.dp)
        ) {
          // Video Title & Stats
          item {
            Column(modifier = Modifier.padding(top = 10.dp, bottom = 6.dp)) {
              Text(
                text = currentVideo.title,
                style = MaterialTheme.typography.titleMedium.copy(
                  fontWeight = FontWeight.Bold,
                  fontSize = 17.sp,
                  color = Color.White
                )
              )

              Spacer(modifier = Modifier.height(4.dp))

              Text(
                text = "${currentVideo.formattedViews} • ${currentVideo.uploadDate}",
                style = MaterialTheme.typography.bodySmall.copy(
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              )
            }
          }

          // Liquid Glass Action Pills Row
          item {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              // Like / Dislike pill
              Row(
                modifier = Modifier
                  .liquidGlassSurface(shape = RoundedCornerShape(20.dp), elevation = 4.dp)
                  .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier
                    .clickable { repository.toggleLike(currentVideo.id) }
                    .testTag("player_like_btn")
                ) {
                  Icon(
                    imageVector = if (currentVideo.isLiked) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                    contentDescription = "Like",
                    tint = if (currentVideo.isLiked) LuminaPink else Color.White,
                    modifier = Modifier.size(16.dp)
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(
                    text = "${currentVideo.likesCount}",
                    style = MaterialTheme.typography.labelSmall.copy(
                      color = Color.White,
                      fontWeight = FontWeight.Bold
                    )
                  )
                }

                Box(
                  modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .width(1.dp)
                    .height(14.dp)
                    .background(Color(0x33FFFFFF))
                )

                Icon(
                  imageVector = if (currentVideo.isDisliked) Icons.Filled.ThumbDown else Icons.Outlined.ThumbDown,
                  contentDescription = "Dislike",
                  tint = if (currentVideo.isDisliked) LuminaCyan else Color.White,
                  modifier = Modifier
                    .size(16.dp)
                    .clickable { repository.toggleDislike(currentVideo.id) }
                    .testTag("player_dislike_btn")
                )
              }

              // Share pill
              ActionPill(
                icon = Icons.Default.Share,
                label = "Share",
                onClick = {
                  val sendIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, "Watch \"${currentVideo.title}\" on Lumina: ${currentVideo.videoUrl}")
                    type = "text/plain"
                  }
                  context.startActivity(Intent.createChooser(sendIntent, "Share video"))
                }
              )

              // Download pill
              ActionPill(
                icon = Icons.Default.Download,
                label = "Download",
                onClick = {
                  repository.startDownload(currentVideo)
                  scope.launch { snackbarHostState.showSnackbar("Video added to offline downloads") }
                }
              )

              // Queue pill
              ActionPill(
                icon = Icons.Default.Queue,
                label = "Queue",
                onClick = {
                  repository.addToQueue(currentVideo)
                  scope.launch { snackbarHostState.showSnackbar("Added to Universal Queue") }
                }
              )
            }
          }

          // Channel Bar
          item {
            GlassCard(
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
              shape = RoundedCornerShape(16.dp),
              elevation = 4.dp
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                AsyncImage(
                  model = currentVideo.channelAvatarUrl,
                  contentDescription = currentVideo.channelTitle,
                  modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .clickable { onOpenChannel(currentVideo.channelId) },
                  contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                      text = currentVideo.channelTitle,
                      style = MaterialTheme.typography.titleSmall.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                      )
                    )
                    if (currentVideo.isVerifiedChannel) {
                      Spacer(modifier = Modifier.width(4.dp))
                      Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Verified",
                        tint = LuminaCyan,
                        modifier = Modifier.size(13.dp)
                      )
                    }
                  }
                  Text(
                    text = "3.8M subscribers",
                    style = MaterialTheme.typography.labelSmall.copy(
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  )
                }

                // Subscribe Button
                Box(
                  modifier = Modifier
                    .background(
                      brush = if (currentVideo.isSubscribed) {
                        Brush.linearGradient(listOf(Color(0x3300F0FF), Color(0x338B5CF6)))
                      } else {
                        Brush.linearGradient(listOf(LuminaCyan, LuminaViolet))
                      },
                      shape = RoundedCornerShape(16.dp)
                    )
                    .clickable { repository.toggleSubscribe(currentVideo.channelId) }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
                    .testTag("player_subscribe_btn"),
                  contentAlignment = Alignment.Center
                ) {
                  Text(
                    text = if (currentVideo.isSubscribed) "Subscribed" else "Subscribe",
                    style = MaterialTheme.typography.labelMedium.copy(
                      color = if (currentVideo.isSubscribed) LuminaCyan else Color(0xFF070A11),
                      fontWeight = FontWeight.Bold
                    )
                  )
                }
              }
            }
          }

          // Feature Tabs Row: 1. Smart Watch Companion, 2. Universal Queue, 3. Comments
          item {
            TabRow(
              selectedTabIndex = selectedTab,
              containerColor = Color.Transparent,
              contentColor = LuminaCyan,
              indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                  modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                  color = LuminaCyan
                )
              },
              modifier = Modifier.padding(top = 10.dp)
            ) {
              Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Companion", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                  }
                }
              )
              Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = {
                  Text("Queue (${universalQueue.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
              )
              Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = {
                  Text("Comments (${videoComments.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
              )
            }
          }

          // TAB 0: SMART WATCH COMPANION (Chapters, AI Notes, Bookmarks)
          if (selectedTab == 0) {
            item {
              SmartWatchCompanionPanel(
                video = currentVideo,
                bookmarks = bookmarks,
                onSeekTo = { seconds ->
                  playerManager.seekTo(seconds * 1000L)
                },
                onAddBookmark = { note ->
                  repository.addBookmark(currentVideo.id, state.currentPositionMs, note)
                },
                onDeleteBookmark = { b ->
                  repository.deleteBookmark(b)
                }
              )
            }
          }

          // TAB 1: UNIVERSAL QUEUE & UP NEXT
          if (selectedTab == 1) {
            item {
              Column(modifier = Modifier.padding(vertical = 10.dp)) {
                if (universalQueue.isNotEmpty()) {
                  Text(
                    text = "Upcoming in Queue",
                    style = MaterialTheme.typography.titleSmall.copy(
                      fontWeight = FontWeight.Bold,
                      color = Color.White
                    ),
                    modifier = Modifier.padding(bottom = 6.dp)
                  )

                  universalQueue.forEach { qVideo ->
                    Row(
                      modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .liquidGlassSurface(shape = RoundedCornerShape(12.dp), elevation = 2.dp)
                        .clickable { onPlayNextVideo(qVideo) }
                        .padding(8.dp),
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      AsyncImage(
                        model = qVideo.thumbnailUrl,
                        contentDescription = null,
                        modifier = Modifier
                          .size(60.dp, 36.dp)
                          .clip(RoundedCornerShape(6.dp)),
                        contentScale = ContentScale.Crop
                      )

                      Spacer(modifier = Modifier.width(8.dp))

                      Column(modifier = Modifier.weight(1f)) {
                        Text(
                          text = qVideo.title,
                          style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                          maxLines = 1,
                          overflow = TextOverflow.Ellipsis
                        )
                        Text(
                          text = qVideo.channelTitle,
                          style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                          )
                        )
                      }

                      IconButton(
                        onClick = { repository.removeFromQueue(qVideo.id) },
                        modifier = Modifier.size(28.dp)
                      ) {
                        Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color.White)
                      }
                    }
                  }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                  text = "Recommended Next",
                  style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                  ),
                  modifier = Modifier.padding(bottom = 6.dp)
                )

                allVideos.filter { it.id != currentVideo.id }.take(5).forEach { nextVid ->
                  Row(
                    modifier = Modifier
                      .fillMaxWidth()
                      .padding(vertical = 6.dp)
                      .clickable { onPlayNextVideo(nextVid) },
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    AsyncImage(
                      model = nextVid.thumbnailUrl,
                      contentDescription = null,
                      modifier = Modifier
                        .size(100.dp, 60.dp)
                        .clip(RoundedCornerShape(8.dp)),
                      contentScale = ContentScale.Crop
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                      Text(
                        text = nextVid.title,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                      )
                      Text(
                        text = "${nextVid.channelTitle} • ${nextVid.formattedViews}",
                        style = MaterialTheme.typography.labelSmall.copy(
                          color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                      )
                    }
                  }
                }
              }
            }
          }

          // TAB 2: COMMENTS
          if (selectedTab == 2) {
            item {
              var newCommentText by remember { mutableStateOf("") }
              Column(modifier = Modifier.padding(vertical = 10.dp)) {
                // Post comment field
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  OutlinedTextField(
                    value = newCommentText,
                    onValueChange = { newCommentText = it },
                    placeholder = { Text("Add a thoughtful comment...", fontSize = 12.sp) },
                    modifier = Modifier.weight(1f).testTag("video_comment_input"),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                      focusedBorderColor = LuminaCyan,
                      unfocusedBorderColor = Color(0x33FFFFFF)
                    )
                  )

                  Spacer(modifier = Modifier.width(8.dp))

                  IconButton(
                    onClick = {
                      if (newCommentText.isNotBlank()) {
                        repository.addComment(currentVideo.id, newCommentText)
                        newCommentText = ""
                      }
                    },
                    modifier = Modifier
                      .size(42.dp)
                      .liquidGlassSurface(shape = CircleShape, elevation = 4.dp)
                      .testTag("video_comment_post_btn")
                  ) {
                    Icon(
                      imageVector = Icons.AutoMirrored.Filled.Send,
                      contentDescription = "Post comment",
                      tint = LuminaCyan,
                      modifier = Modifier.size(18.dp)
                    )
                  }
                }

                // Comments list
                videoComments.forEach { comment ->
                  Row(
                    modifier = Modifier
                      .fillMaxWidth()
                      .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.Top
                  ) {
                    AsyncImage(
                      model = comment.authorAvatarUrl,
                      contentDescription = null,
                      modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape),
                      contentScale = ContentScale.Crop
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                      Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                          text = comment.authorName,
                          style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                          )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                          text = comment.timestamp,
                          style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                          )
                        )
                      }

                      Spacer(modifier = Modifier.height(2.dp))

                      Text(
                        text = comment.text,
                        style = MaterialTheme.typography.bodySmall.copy(
                          color = Color(0xFFE2E8F0)
                        )
                      )
                    }

                    IconButton(
                      onClick = { repository.toggleCommentLike(currentVideo.id, comment.id) },
                      modifier = Modifier.size(28.dp)
                    ) {
                      Icon(
                        imageVector = if (comment.isLiked) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                        contentDescription = "Like",
                        tint = if (comment.isLiked) LuminaCyan else Color.White,
                        modifier = Modifier.size(14.dp)
                      )
                    }
                  }
                }
              }
            }
          }

          item { Spacer(modifier = Modifier.height(80.dp)) }
        }
      }
    }

    SnackbarHost(
      hostState = snackbarHostState,
      modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 70.dp)
    )
  }
}

@Composable
fun SmartWatchCompanionPanel(
  video: Video,
  bookmarks: List<VideoBookmark>,
  onSeekTo: (Int) -> Unit,
  onAddBookmark: (String) -> Unit,
  onDeleteBookmark: (VideoBookmark) -> Unit
) {
  val scope = rememberCoroutineScope()
  var aiSummary by remember { mutableStateOf<String?>(null) }
  var isAiLoading by remember { mutableStateOf(false) }
  var userQuestion by remember { mutableStateOf("") }
  var aiAnswer by remember { mutableStateOf<String?>(null) }
  var isAnswering by remember { mutableStateOf(false) }

  var newBookmarkNote by remember { mutableStateOf("") }

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 12.dp)
  ) {
    // CHAPTER NAVIGATION
    if (video.chapters.isNotEmpty()) {
      Text(
        text = "Interactive Chapters (${video.chapters.size})",
        style = MaterialTheme.typography.titleSmall.copy(
          fontWeight = FontWeight.Bold,
          color = Color.White
        ),
        modifier = Modifier.padding(bottom = 8.dp)
      )

      video.chapters.forEach { chapter ->
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .liquidGlassSurface(shape = RoundedCornerShape(12.dp), elevation = 2.dp)
            .clickable { onSeekTo(chapter.startTimeSeconds) }
            .padding(horizontal = 12.dp, vertical = 10.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .background(Color(0x3300F0FF), RoundedCornerShape(6.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
              Text(
                text = chapter.formattedTime,
                style = MaterialTheme.typography.labelSmall.copy(
                  color = LuminaCyan,
                  fontWeight = FontWeight.Bold
                )
              )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
              text = chapter.title,
              style = MaterialTheme.typography.bodySmall.copy(
                color = Color.White,
                fontWeight = FontWeight.Medium
              )
            )
          }

          Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = "Jump",
            tint = LuminaCyan,
            modifier = Modifier.size(16.dp)
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // AI STUDY NOTES & SYNTHESIS
    GlassCard(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(16.dp),
      elevation = 6.dp
    ) {
      Column(modifier = Modifier.padding(14.dp)) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.AutoAwesome,
              contentDescription = null,
              tint = LuminaCyan,
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Lumina AI Companion",
              style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Bold,
                color = Color.White
              )
            )
          }

          Box(
            modifier = Modifier
              .background(
                Brush.horizontalGradient(listOf(LuminaCyan, LuminaViolet)),
                RoundedCornerShape(12.dp)
              )
              .clickable {
                scope.launch {
                  isAiLoading = true
                  aiSummary = GeminiAssistant.generateVideoSummary(video)
                  isAiLoading = false
                }
              }
              .padding(horizontal = 10.dp, vertical = 5.dp)
              .testTag("ai_generate_summary_btn")
          ) {
            Text(
              text = if (isAiLoading) "Thinking..." else "Generate Notes",
              style = MaterialTheme.typography.labelSmall.copy(
                color = Color(0xFF070A11),
                fontWeight = FontWeight.Bold
              )
            )
          }
        }

        if (isAiLoading) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .padding(16.dp),
            contentAlignment = Alignment.Center
          ) {
            CircularProgressIndicator(color = LuminaCyan, modifier = Modifier.size(24.dp))
          }
        } else if (aiSummary != null) {
          Spacer(modifier = Modifier.height(10.dp))
          Text(
            text = aiSummary ?: "",
            style = MaterialTheme.typography.bodySmall.copy(
              color = Color(0xFFF1F5F9),
              lineHeight = 18.sp
            )
          )
        } else {
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = "Tap 'Generate Notes' to summarize key concepts, timestamps, and study takeaways with Lumina's intelligence.",
            style = MaterialTheme.typography.bodySmall.copy(
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Ask Q&A Box
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically
        ) {
          OutlinedTextField(
            value = userQuestion,
            onValueChange = { userQuestion = it },
            placeholder = { Text("Ask anything about this video...", fontSize = 11.sp) },
            modifier = Modifier.weight(1f).testTag("ai_question_input"),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = LuminaCyan,
              unfocusedBorderColor = Color(0x33FFFFFF)
            )
          )

          Spacer(modifier = Modifier.width(6.dp))

          IconButton(
            onClick = {
              if (userQuestion.isNotBlank()) {
                scope.launch {
                  isAnswering = true
                  aiAnswer = GeminiAssistant.askQuestionAboutVideo(video, userQuestion)
                  isAnswering = false
                }
              }
            },
            modifier = Modifier
              .size(38.dp)
              .liquidGlassSurface(shape = CircleShape, elevation = 4.dp)
              .testTag("ai_ask_btn")
          ) {
            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Ask", tint = LuminaCyan, modifier = Modifier.size(16.dp))
          }
        }

        if (isAnswering) {
          Box(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            contentAlignment = Alignment.Center
          ) {
            CircularProgressIndicator(color = LuminaCyan, modifier = Modifier.size(20.dp))
          }
        } else if (aiAnswer != null) {
          Spacer(modifier = Modifier.height(8.dp))
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .background(Color(0x3300F0FF), RoundedCornerShape(10.dp))
              .padding(10.dp)
          ) {
            Text(
              text = "💬 AI: ${aiAnswer ?: ""}",
              style = MaterialTheme.typography.bodySmall.copy(color = Color.White)
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // BOOKMARKS & TIMESTAMPS
    Text(
      text = "Your Bookmarks (${bookmarks.size})",
      style = MaterialTheme.typography.titleSmall.copy(
        fontWeight = FontWeight.Bold,
        color = Color.White
      ),
      modifier = Modifier.padding(bottom = 6.dp)
    )

    Row(
      modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      OutlinedTextField(
        value = newBookmarkNote,
        onValueChange = { newBookmarkNote = it },
        placeholder = { Text("Add bookmark note at current time...", fontSize = 12.sp) },
        modifier = Modifier.weight(1f).testTag("bookmark_note_input"),
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
          focusedBorderColor = LuminaCyan,
          unfocusedBorderColor = Color(0x33FFFFFF)
        )
      )

      Spacer(modifier = Modifier.width(8.dp))

      Box(
        modifier = Modifier
          .liquidGlassSurface(shape = RoundedCornerShape(14.dp), elevation = 4.dp)
          .clickable {
            onAddBookmark(newBookmarkNote)
            newBookmarkNote = ""
          }
          .padding(horizontal = 12.dp, vertical = 10.dp)
          .testTag("bookmark_add_btn")
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.Bookmark, contentDescription = null, tint = LuminaCyan, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Pin", color = LuminaCyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
      }
    }

    bookmarks.forEach { bookmark ->
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 4.dp)
          .liquidGlassSurface(shape = RoundedCornerShape(10.dp), elevation = 2.dp)
          .clickable {
            val seconds = (bookmark.timestampMs / 1000).toInt()
            onSeekTo(seconds)
          }
          .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = bookmark.formattedTime,
            style = MaterialTheme.typography.labelSmall.copy(
              color = LuminaCyan,
              fontWeight = FontWeight.Bold
            )
          )
          Spacer(modifier = Modifier.width(10.dp))
          Text(
            text = bookmark.note,
            style = MaterialTheme.typography.bodySmall.copy(color = Color.White)
          )
        }

        IconButton(
          onClick = { onDeleteBookmark(bookmark) },
          modifier = Modifier.size(26.dp)
        ) {
          Icon(Icons.Default.Close, contentDescription = "Delete", tint = Color.Gray, modifier = Modifier.size(14.dp))
        }
      }
    }
  }
}

@Composable
fun ActionPill(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  label: String,
  onClick: () -> Unit
) {
  Row(
    modifier = Modifier
      .liquidGlassSurface(shape = RoundedCornerShape(20.dp), elevation = 4.dp)
      .clickable { onClick() }
      .padding(horizontal = 12.dp, vertical = 6.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Icon(
      imageVector = icon,
      contentDescription = label,
      tint = Color.White,
      modifier = Modifier.size(16.dp)
    )
    Spacer(modifier = Modifier.width(6.dp))
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall.copy(
        color = Color.White,
        fontWeight = FontWeight.Medium
      )
    )
  }
}

private fun formatMs(ms: Long): String {
  val totalSeconds = (ms / 1000).coerceAtLeast(0L)
  val minutes = totalSeconds / 60
  val seconds = totalSeconds % 60
  return String.format(Locale.US, "%d:%02d", minutes, seconds)
}
