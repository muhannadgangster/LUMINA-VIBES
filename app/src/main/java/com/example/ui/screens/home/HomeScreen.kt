package com.example.ui.screens.home

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.HistoryEntry
import com.example.data.model.Video
import com.example.data.repository.VideoRepository
import com.example.ui.components.VideoItemCard
import com.example.ui.glass.GlassCard
import com.example.ui.glass.GlassIconButton
import com.example.ui.glass.liquidGlassSurface
import com.example.ui.theme.LuminaCyan
import com.example.ui.theme.LuminaPink
import com.example.ui.theme.LuminaViolet
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
  repository: VideoRepository,
  onPlayVideo: (Video) -> Unit,
  onOpenShorts: (String?) -> Unit,
  onOpenSearch: () -> Unit,
  onOpenChannel: (String) -> Unit,
  onOpenNotifications: () -> Unit,
  onOpenProfile: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val snackbarHostState = remember { SnackbarHostState() }

  val allVideos by repository.videos.collectAsState()
  val allShorts by repository.shorts.collectAsState()
  val watchHistory by repository.watchHistory.collectAsState(initial = emptyList())

  var selectedCategory by remember { mutableStateOf("All") }
  val categories = listOf("All", "Sci-Fi & Cinema", "Animation", "Gaming & Tech", "Space & Science", "Nature")

  val filteredVideos = remember(allVideos, selectedCategory) {
    if (selectedCategory == "All") allVideos else allVideos.filter { it.category == selectedCategory }
  }

  val heroVideo = allVideos.firstOrNull()

  // Continue Watching list
  val continueWatchingVideos = remember(watchHistory, allVideos) {
    watchHistory.mapNotNull { entry ->
      allVideos.find { it.id == entry.videoId }?.let { video ->
        Pair(video, entry)
      }
    }.take(6)
  }

  Scaffold(
    modifier = modifier.fillMaxSize(),
    containerColor = Color.Transparent,
    snackbarHost = { SnackbarHost(snackbarHostState) },
    topBar = {
      // Liquid Glass Top Bar
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 8.dp)
          .liquidGlassSurface(shape = RoundedCornerShape(20.dp), elevation = 6.dp)
          .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        // App Logo & Brand
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.weight(1f)
        ) {
          Box(
            modifier = Modifier
              .size(34.dp)
              .clip(CircleShape)
              .background(
                Brush.linearGradient(
                  listOf(LuminaCyan, LuminaViolet)
                )
              ),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.PlayArrow,
              contentDescription = "Lumina Logo",
              tint = Color(0xFF070A11),
              modifier = Modifier.size(20.dp)
            )
          }

          Spacer(modifier = Modifier.width(10.dp))

          Text(
            text = "LUMINA",
            style = MaterialTheme.typography.titleLarge.copy(
              fontWeight = FontWeight.Black,
              letterSpacing = 1.5.sp,
              brush = Brush.horizontalGradient(
                listOf(LuminaCyan, Color.White, LuminaViolet)
              )
            )
          )
        }

        // Actions: Cast, Notifications, Search, Profile Avatar
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          IconButton(
            onClick = {
              scope.launch { snackbarHostState.showSnackbar("Ready to cast to display") }
            },
            modifier = Modifier.size(36.dp).testTag("action_cast")
          ) {
            Icon(
              imageVector = Icons.Default.Cast,
              contentDescription = "Cast",
              tint = Color.White,
              modifier = Modifier.size(20.dp)
            )
          }

          IconButton(
            onClick = onOpenNotifications,
            modifier = Modifier.size(36.dp).testTag("action_notifications")
          ) {
            Box {
              Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = "Notifications",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
              )
              // Notification dot
              Box(
                modifier = Modifier
                  .size(8.dp)
                  .align(Alignment.TopEnd)
                  .clip(CircleShape)
                  .background(LuminaPink)
              )
            }
          }

          IconButton(
            onClick = onOpenSearch,
            modifier = Modifier.size(36.dp).testTag("action_search")
          ) {
            Icon(
              imageVector = Icons.Default.Search,
              contentDescription = "Search",
              tint = Color.White,
              modifier = Modifier.size(20.dp)
            )
          }

          // User Profile Avatar
          AsyncImage(
            model = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150&auto=format&fit=crop&q=80",
            contentDescription = "Profile",
            modifier = Modifier
              .size(32.dp)
              .clip(CircleShape)
              .clickable { onOpenProfile() }
              .testTag("action_profile_avatar"),
            contentScale = ContentScale.Crop
          )
        }
      }
    }
  ) { innerPadding ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding),
      contentPadding = PaddingValues(bottom = 100.dp)
    ) {
      // Category Filter Chips
      item {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 6.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          categories.forEach { cat ->
            val isSelected = cat == selectedCategory
            val backgroundModifier = if (isSelected) {
              Modifier
                .background(
                  brush = Brush.horizontalGradient(listOf(LuminaCyan, LuminaViolet)),
                  shape = RoundedCornerShape(20.dp)
                )
            } else {
              Modifier.liquidGlassSurface(shape = RoundedCornerShape(20.dp), elevation = 2.dp)
            }

            Box(
              modifier = Modifier
                .then(backgroundModifier)
                .clickable { selectedCategory = cat }
                .padding(horizontal = 14.dp, vertical = 8.dp)
                .testTag("filter_chip_$cat"),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = cat,
                style = MaterialTheme.typography.labelMedium.copy(
                  color = if (isSelected) Color(0xFF050811) else Color.White,
                  fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
              )
            }
          }
        }
      }

      // Featured Spotlight Hero (shown when "All" is active)
      if (selectedCategory == "All" && heroVideo != null) {
        item {
          GlassCard(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(20.dp),
            elevation = 8.dp,
            onClick = { onPlayVideo(heroVideo) },
            testTag = "featured_hero_card"
          ) {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .height(210.dp)
            ) {
              AsyncImage(
                model = heroVideo.thumbnailUrl,
                contentDescription = heroVideo.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
              )

              // Dark & iridescent scrim
              Box(
                modifier = Modifier
                  .fillMaxSize()
                  .background(
                    Brush.verticalGradient(
                      colors = listOf(
                        Color.Transparent,
                        Color(0x80000000),
                        Color(0xE6070A11)
                      )
                    )
                  )
              )

              // Badge
              Box(
                modifier = Modifier
                  .padding(12.dp)
                  .align(Alignment.TopStart)
                  .background(
                    Brush.horizontalGradient(listOf(LuminaPink, LuminaViolet)),
                    RoundedCornerShape(8.dp)
                  )
                  .padding(horizontal = 8.dp, vertical = 4.dp)
              ) {
                Text(
                  text = "FEATURED PREMIERE",
                  style = MaterialTheme.typography.labelSmall.copy(
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                  )
                )
              }

              // Info & Play Action
              Column(
                modifier = Modifier
                  .align(Alignment.BottomStart)
                  .padding(16.dp)
              ) {
                Text(
                  text = heroVideo.title,
                  style = MaterialTheme.typography.titleMedium.copy(
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                  ),
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                  Text(
                    text = "${heroVideo.channelTitle} • ${heroVideo.formattedDuration}",
                    style = MaterialTheme.typography.bodySmall.copy(
                      color = LuminaCyan,
                      fontWeight = FontWeight.Medium
                    )
                  )

                  // Glowing Glass Play Button
                  Row(
                    modifier = Modifier
                      .background(
                        Brush.horizontalGradient(listOf(LuminaCyan, LuminaViolet)),
                        RoundedCornerShape(14.dp)
                      )
                      .padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Icon(
                      imageVector = Icons.Default.PlayArrow,
                      contentDescription = null,
                      tint = Color(0xFF070A11),
                      modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                      text = "Watch",
                      style = MaterialTheme.typography.labelMedium.copy(
                        color = Color(0xFF070A11),
                        fontWeight = FontWeight.Bold
                      )
                    )
                  }
                }
              }
            }
          }
        }
      }

      // Continue Watching Shelf
      if (continueWatchingVideos.isNotEmpty() && selectedCategory == "All") {
        item {
          Column(modifier = Modifier.padding(vertical = 8.dp)) {
            Text(
              text = "Continue Watching",
              style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = Color.White
              ),
              modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            LazyRow(
              contentPadding = PaddingValues(horizontal = 16.dp),
              horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
              items(continueWatchingVideos) { (video, entry) ->
                val progress = if (entry.durationMs > 0) {
                  (entry.playbackPositionMs.toFloat() / entry.durationMs.toFloat()).coerceIn(0f, 1f)
                } else 0.5f

                GlassCard(
                  modifier = Modifier.width(190.dp),
                  shape = RoundedCornerShape(14.dp),
                  elevation = 4.dp,
                  onClick = { onPlayVideo(video) }
                ) {
                  Column {
                    Box(
                      modifier = Modifier
                        .fillMaxWidth()
                        .height(105.dp)
                    ) {
                      AsyncImage(
                        model = video.thumbnailUrl,
                        contentDescription = video.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                      )
                      LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                          .fillMaxWidth()
                          .height(3.dp)
                          .align(Alignment.BottomCenter),
                        color = LuminaCyan,
                        trackColor = Color(0x66000000)
                      )
                    }

                    Column(modifier = Modifier.padding(8.dp)) {
                      Text(
                        text = video.title,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                      )
                      Text(
                        text = video.channelTitle,
                        style = MaterialTheme.typography.labelSmall.copy(
                          color = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        maxLines = 1
                      )
                    }
                  }
                }
              }
            }
          }
        }
      }

      // Shorts Shelf
      if (allShorts.isNotEmpty() && selectedCategory == "All") {
        item {
          Column(modifier = Modifier.padding(vertical = 12.dp)) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                  text = "⚡ Lumina Shorts",
                  style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                  )
                )
              }
              Text(
                text = "View all",
                style = MaterialTheme.typography.labelMedium.copy(
                  color = LuminaCyan,
                  fontWeight = FontWeight.SemiBold
                ),
                modifier = Modifier.clickable { onOpenShorts(null) }
              )
            }

            LazyRow(
              contentPadding = PaddingValues(horizontal = 16.dp),
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              items(allShorts) { short ->
                GlassCard(
                  modifier = Modifier
                    .width(135.dp)
                    .height(220.dp),
                  shape = RoundedCornerShape(16.dp),
                  elevation = 6.dp,
                  onClick = { onOpenShorts(short.id) },
                  testTag = "short_item_${short.id}"
                ) {
                  Box(modifier = Modifier.fillMaxSize()) {
                    AsyncImage(
                      model = short.thumbnailUrl,
                      contentDescription = short.title,
                      modifier = Modifier.fillMaxSize(),
                      contentScale = ContentScale.Crop
                    )

                    // Scrim
                    Box(
                      modifier = Modifier
                        .fillMaxSize()
                        .background(
                          Brush.verticalGradient(
                            listOf(Color.Transparent, Color(0xCC050811))
                          )
                        )
                    )

                    // Short title & views
                    Column(
                      modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(10.dp)
                    ) {
                      Text(
                        text = short.title,
                        style = MaterialTheme.typography.bodySmall.copy(
                          color = Color.White,
                          fontWeight = FontWeight.Bold,
                          fontSize = 12.sp
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                      )
                      Spacer(modifier = Modifier.height(2.dp))
                      Text(
                        text = short.formattedViews,
                        style = MaterialTheme.typography.labelSmall.copy(
                          color = LuminaCyan,
                          fontSize = 10.sp
                        )
                      )
                    }
                  }
                }
              }
            }
          }
        }
      }

      // Main Videos Feed
      items(filteredVideos) { video ->
        VideoItemCard(
          video = video,
          onClick = { onPlayVideo(video) },
          onChannelClick = { onOpenChannel(video.channelId) },
          onSaveToWatchLater = {
            repository.toggleWatchLater(video.id)
            scope.launch { snackbarHostState.showSnackbar("Saved to Watch Later") }
          },
          onAddToQueue = {
            repository.addToQueue(video)
            scope.launch { snackbarHostState.showSnackbar("Added to Universal Queue") }
          },
          onShare = {
            val sendIntent = Intent().apply {
              action = Intent.ACTION_SEND
              putExtra(Intent.EXTRA_TEXT, "Watch \"${video.title}\" on Lumina: ${video.videoUrl}")
              type = "text/plain"
            }
            context.startActivity(Intent.createChooser(sendIntent, "Share video"))
          }
        )
      }
    }
  }
}
