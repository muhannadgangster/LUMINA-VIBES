@file:OptIn(
  androidx.media3.common.util.UnstableApi::class,
  androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.example.ui.screens.shorts

import android.content.Context
import android.content.Intent
import android.net.Uri
import kotlin.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
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
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.example.data.model.Comment
import com.example.data.model.Video
import com.example.data.repository.VideoRepository
import com.example.ui.glass.liquidGlassSurface
import com.example.ui.theme.LuminaCyan
import com.example.ui.theme.LuminaPink
import com.example.ui.theme.LuminaViolet

@OptIn(UnstableApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ShortsScreen(
  repository: VideoRepository,
  initialShortId: String? = null,
  onOpenChannel: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val shorts by repository.shorts.collectAsState()

  if (shorts.isEmpty()) {
    Box(
      modifier = modifier.fillMaxSize().background(Color(0xFF070A11)),
      contentAlignment = Alignment.Center
    ) {
      Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = LuminaCyan, modifier = Modifier.size(40.dp))
        Spacer(modifier = Modifier.height(12.dp))
        Text(
          text = "No publisher yet publish video",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White),
          modifier = Modifier.testTag("shorts_empty_state_text")
        )
      }
    }
    return
  }

  val initialIndex = remember(initialShortId, shorts) {
    if (initialShortId != null) {
      val found = shorts.indexOfFirst { it.id == initialShortId }
      if (found >= 0) found else 0
    } else 0
  }

  val pagerState = rememberPagerState(initialPage = initialIndex, pageCount = { shorts.size })

  // Dedicated ExoPlayer for Shorts
  val shortsPlayer = remember {
    val httpFactory = androidx.media3.datasource.DefaultHttpDataSource.Factory()
      .setUserAgent("Mozilla/5.0 (Linux; Android 14; Lumina/1.0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
      .setConnectTimeoutMs(15000)
      .setReadTimeoutMs(15000)
      .setAllowCrossProtocolRedirects(true)
    val dsFactory = androidx.media3.datasource.DefaultDataSource.Factory(context, httpFactory)
    ExoPlayer.Builder(context)
      .setMediaSourceFactory(androidx.media3.exoplayer.source.DefaultMediaSourceFactory(dsFactory))
      .build().apply {
        repeatMode = Player.REPEAT_MODE_ONE
      }
  }

  DisposableEffect(Unit) {
    onDispose {
      shortsPlayer.release()
    }
  }

  // Play video on current page change
  LaunchedEffect(pagerState.currentPage, shorts) {
    if (shorts.isNotEmpty() && pagerState.currentPage in shorts.indices) {
      val activeShort = shorts[pagerState.currentPage]
      val item = MediaItem.fromUri(Uri.parse(activeShort.videoUrl))
      shortsPlayer.setMediaItem(item)
      shortsPlayer.prepare()
      shortsPlayer.play()
    }
  }

  var showCommentsSheet by remember { mutableStateOf(false) }
  var currentShortForComments by remember { mutableStateOf<Video?>(null) }

  VerticalPager(
    state = pagerState,
    modifier = modifier.fillMaxSize().testTag("shorts_vertical_pager")
  ) { page ->
    val short = shorts[page]
    var isPaused by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
      // Background / Poster
      AsyncImage(
        model = short.thumbnailUrl,
        contentDescription = null,
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Crop
      )

      // Active video playback surface
      if (pagerState.currentPage == page) {
        AndroidView(
          factory = { ctx ->
            PlayerView(ctx).apply {
              player = shortsPlayer
              useController = false
              resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            }
          },
          modifier = Modifier
            .fillMaxSize()
            .clickable(
              interactionSource = remember { MutableInteractionSource() },
              indication = null
            ) {
              if (shortsPlayer.isPlaying) {
                shortsPlayer.pause()
                isPaused = true
              } else {
                shortsPlayer.play()
                isPaused = false
              }
            }
        )
      }

      // Pause indicator overlay
      AnimatedVisibility(
        visible = isPaused,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = Modifier.align(Alignment.Center)
      ) {
        Box(
          modifier = Modifier
            .size(72.dp)
            .liquidGlassSurface(shape = CircleShape, elevation = 8.dp),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = "Paused",
            tint = LuminaCyan,
            modifier = Modifier.size(42.dp)
          )
        }
      }

      // Bottom Scrim
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(280.dp)
          .align(Alignment.BottomCenter)
          .background(
            Brush.verticalGradient(
              listOf(Color.Transparent, Color(0xCC050811), Color(0xF2070A11))
            )
          )
      )

      // Right Floating Action Column (Liquid Glass pills)
      Column(
        modifier = Modifier
          .align(Alignment.BottomEnd)
          .padding(end = 12.dp, bottom = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        // Like Action
        ShortsActionButton(
          icon = if (short.isLiked) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
          label = "${short.likesCount}",
          isActive = short.isLiked,
          activeColor = LuminaPink,
          testTag = "short_like_${short.id}",
          onClick = { repository.toggleLike(short.id) }
        )

        // Dislike Action
        ShortsActionButton(
          icon = if (short.isDisliked) Icons.Filled.ThumbDown else Icons.Outlined.ThumbDown,
          label = "Dislike",
          isActive = short.isDisliked,
          testTag = "short_dislike_${short.id}",
          onClick = { repository.toggleDislike(short.id) }
        )

        // Comments Action
        ShortsActionButton(
          icon = Icons.AutoMirrored.Filled.Comment,
          label = "Comments",
          testTag = "short_comment_${short.id}",
          onClick = {
            currentShortForComments = short
            showCommentsSheet = true
          }
        )

        // Share Action
        ShortsActionButton(
          icon = Icons.Default.Share,
          label = "Share",
          testTag = "short_share_${short.id}",
          onClick = {
            val shareIntent = Intent().apply {
              action = Intent.ACTION_SEND
              putExtra(Intent.EXTRA_TEXT, "Watch \"${short.title}\" on Lumina Shorts: ${short.videoUrl}")
              type = "text/plain"
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share short"))
          }
        )

        // Save Action
        ShortsActionButton(
          icon = Icons.Default.Bookmark,
          label = "Save",
          testTag = "short_save_${short.id}",
          onClick = {
            repository.createCollection("Shorts Favorites", "Saved shorts collection", listOf(short.id))
          }
        )

        // Spinning Music Audio Disc
        SpinningAudioDisc(avatarUrl = short.channelAvatarUrl)
      }

      // Bottom Left Creator & Title Info
      Column(
        modifier = Modifier
          .align(Alignment.BottomStart)
          .fillMaxWidth(0.78f)
          .padding(start = 16.dp, bottom = 80.dp)
      ) {
        // Channel Bar
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.padding(bottom = 8.dp)
        ) {
          AsyncImage(
            model = short.channelAvatarUrl,
            contentDescription = short.channelTitle,
            modifier = Modifier
              .size(38.dp)
              .clip(CircleShape)
              .clickable { onOpenChannel(short.channelId) },
            contentScale = ContentScale.Crop
          )

          Spacer(modifier = Modifier.width(8.dp))

          Text(
            text = short.channelTitle,
            style = MaterialTheme.typography.titleMedium.copy(
              color = Color.White,
              fontWeight = FontWeight.Bold,
              fontSize = 14.sp
            ),
            modifier = Modifier.clickable { onOpenChannel(short.channelId) }
          )

          if (short.isVerifiedChannel) {
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
              imageVector = Icons.Default.CheckCircle,
              contentDescription = "Verified",
              tint = LuminaCyan,
              modifier = Modifier.size(14.dp)
            )
          }

          Spacer(modifier = Modifier.width(10.dp))

          // Subscribe Pill
          Box(
            modifier = Modifier
              .liquidGlassSurface(
                shape = RoundedCornerShape(14.dp),
                elevation = 4.dp
              )
              .clickable { repository.toggleSubscribe(short.channelId) }
              .padding(horizontal = 10.dp, vertical = 4.dp)
              .testTag("short_subscribe_${short.channelId}")
          ) {
            Text(
              text = if (short.isSubscribed) "Subscribed" else "Subscribe",
              style = MaterialTheme.typography.labelSmall.copy(
                color = if (short.isSubscribed) LuminaCyan else Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
              )
            )
          }
        }

        // Title and description
        var isExpanded by remember { mutableStateOf(false) }
        Text(
          text = short.title,
          style = MaterialTheme.typography.bodyMedium.copy(
            color = Color.White,
            fontWeight = FontWeight.Medium
          ),
          maxLines = if (isExpanded) 6 else 2,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.clickable { isExpanded = !isExpanded }
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Sound Track Badge
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier
            .liquidGlassSurface(shape = RoundedCornerShape(12.dp), elevation = 2.dp)
            .padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
          Icon(
            imageVector = Icons.Default.MusicNote,
            contentDescription = null,
            tint = LuminaCyan,
            modifier = Modifier.size(13.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = "${short.channelTitle} • Original Audio",
            style = MaterialTheme.typography.labelSmall.copy(
              color = Color.White,
              fontSize = 11.sp
            ),
            maxLines = 1
          )
        }
      }
    }
  }

  // Comments Bottom Sheet
  if (showCommentsSheet && currentShortForComments != null) {
    val sheetState = rememberModalBottomSheetState()
    val targetShort = currentShortForComments!!
    val allCommentsMap by repository.comments.collectAsState()
    val videoComments = allCommentsMap[targetShort.id] ?: emptyList()
    var newCommentText by remember { mutableStateOf("") }

    ModalBottomSheet(
      onDismissRequest = { showCommentsSheet = false },
      sheetState = sheetState,
      containerColor = Color(0xF20E1526)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 8.dp)
      ) {
        Text(
          text = "Comments (${videoComments.size})",
          style = MaterialTheme.typography.titleMedium.copy(
            color = Color.White,
            fontWeight = FontWeight.Bold
          ),
          modifier = Modifier.padding(bottom = 12.dp)
        )

        LazyColumn(
          modifier = Modifier
            .weight(1f, fill = false)
            .height(350.dp)
        ) {
          if (videoComments.isEmpty()) {
            item {
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(32.dp),
                contentAlignment = Alignment.Center
              ) {
                Text(
                  "No comments yet. Be the first to chime in!",
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }
          } else {
            items(videoComments) { comment ->
              CommentRow(comment = comment, onLike = {
                repository.toggleCommentLike(targetShort.id, comment.id)
              })
            }
          }
        }

        // Add Comment Input
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 20.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          OutlinedTextField(
            value = newCommentText,
            onValueChange = { newCommentText = it },
            placeholder = { Text("Add a comment...", fontSize = 13.sp) },
            modifier = Modifier.weight(1f).testTag("short_comment_input"),
            shape = RoundedCornerShape(20.dp),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = LuminaCyan,
              unfocusedBorderColor = Color(0x33FFFFFF)
            )
          )

          Spacer(modifier = Modifier.width(8.dp))

          IconButton(
            onClick = {
              if (newCommentText.isNotBlank()) {
                repository.addComment(targetShort.id, newCommentText)
                newCommentText = ""
              }
            },
            modifier = Modifier
              .size(42.dp)
              .liquidGlassSurface(shape = CircleShape, elevation = 4.dp)
              .testTag("short_comment_send")
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.Send,
              contentDescription = "Send",
              tint = LuminaCyan,
              modifier = Modifier.size(18.dp)
            )
          }
        }
      }
    }
  }
}

@Composable
fun ShortsActionButton(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  label: String,
  onClick: () -> Unit,
  isActive: Boolean = false,
  activeColor: Color = LuminaCyan,
  testTag: String = "shorts_action_btn"
) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = Modifier.clickable { onClick() }.testTag(testTag)
  ) {
    Box(
      modifier = Modifier
        .size(44.dp)
        .liquidGlassSurface(shape = CircleShape, elevation = 6.dp),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = icon,
        contentDescription = label,
        tint = if (isActive) activeColor else Color.White,
        modifier = Modifier.size(22.dp)
      )
    }

    Spacer(modifier = Modifier.height(4.dp))

    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall.copy(
        color = Color.White,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp
      )
    )
  }
}

@Composable
fun SpinningAudioDisc(avatarUrl: String) {
  val infiniteTransition = rememberInfiniteTransition(label = "discSpin")
  val rotation by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 360f,
    animationSpec = infiniteRepeatable(
      animation = tween(4000, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "discRotation"
  )

  Box(
    modifier = Modifier
      .size(44.dp)
      .rotate(rotation)
      .liquidGlassSurface(shape = CircleShape, elevation = 6.dp)
      .padding(6.dp),
    contentAlignment = Alignment.Center
  ) {
    AsyncImage(
      model = avatarUrl,
      contentDescription = "Audio track",
      modifier = Modifier
        .fillMaxSize()
        .clip(CircleShape),
      contentScale = ContentScale.Crop
    )
  }
}

@Composable
fun CommentRow(comment: Comment, onLike: () -> Unit) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 8.dp),
    verticalAlignment = Alignment.Top
  ) {
    AsyncImage(
      model = comment.authorAvatarUrl,
      contentDescription = comment.authorName,
      modifier = Modifier
        .size(32.dp)
        .clip(CircleShape),
      contentScale = ContentScale.Crop
    )

    Spacer(modifier = Modifier.width(10.dp))

    Column(modifier = Modifier.weight(1f)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
          text = comment.authorName,
          style = MaterialTheme.typography.labelMedium.copy(
            color = Color.White,
            fontWeight = FontWeight.Bold
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
        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFE2E8F0))
      )
    }

    IconButton(onClick = onLike, modifier = Modifier.size(32.dp)) {
      Icon(
        imageVector = if (comment.isLiked) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
        contentDescription = "Like comment",
        tint = if (comment.isLiked) LuminaCyan else Color.White,
        modifier = Modifier.size(15.dp)
      )
    }
  }
}
