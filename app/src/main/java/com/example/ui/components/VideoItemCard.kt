package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Queue
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.WatchLater
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.ui.glass.GlassCard
import com.example.ui.theme.LuminaCyan

@Composable
fun VideoItemCard(
  video: Video,
  onClick: () -> Unit,
  onChannelClick: (String) -> Unit,
  onSaveToWatchLater: () -> Unit,
  onAddToQueue: () -> Unit,
  onShare: () -> Unit,
  modifier: Modifier = Modifier
) {
  var menuExpanded by remember { mutableStateOf(false) }

  GlassCard(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 8.dp),
    shape = RoundedCornerShape(16.dp),
    elevation = 4.dp,
    onClick = onClick,
    testTag = "video_card_${video.id}"
  ) {
    Column {
      // Thumbnail Box
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .aspectRatio(16f / 9f)
          .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
      ) {
        AsyncImage(
          model = video.thumbnailUrl,
          contentDescription = video.title,
          modifier = Modifier.matchParentSize(),
          contentScale = ContentScale.Crop
        )

        // Gradient overlay at bottom of thumbnail
        Box(
          modifier = Modifier
            .matchParentSize()
            .background(
              Brush.verticalGradient(
                colors = listOf(Color.Transparent, Color(0x99000000)),
                startY = 180f
              )
            )
        )

        // Duration Badge
        Box(
          modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(8.dp)
            .background(Color(0xCC050811), RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 3.dp)
        ) {
          Text(
            text = video.formattedDuration,
            style = MaterialTheme.typography.labelSmall.copy(
              color = Color.White,
              fontWeight = FontWeight.SemiBold,
              fontSize = 11.sp
            )
          )
        }

        // Category Tag
        Box(
          modifier = Modifier
            .align(Alignment.TopStart)
            .padding(8.dp)
            .background(Color(0x990B1220), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
          Text(
            text = video.category,
            style = MaterialTheme.typography.labelSmall.copy(
              color = LuminaCyan,
              fontWeight = FontWeight.Medium,
              fontSize = 10.sp
            )
          )
        }
      }

      // Metadata Row
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(12.dp),
        verticalAlignment = Alignment.Top
      ) {
        // Channel Avatar
        AsyncImage(
          model = video.channelAvatarUrl,
          contentDescription = video.channelTitle,
          modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .clickable { onChannelClick(video.channelId) },
          contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Title and details
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = video.title,
            style = MaterialTheme.typography.titleMedium.copy(
              fontWeight = FontWeight.SemiBold,
              fontSize = 14.sp
            ),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
          )

          Spacer(modifier = Modifier.height(4.dp))

          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            Text(
              text = video.channelTitle,
              style = MaterialTheme.typography.bodySmall.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
              ),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )

            if (video.isVerifiedChannel) {
              Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Verified Channel",
                tint = LuminaCyan,
                modifier = Modifier.size(13.dp)
              )
            }

            Text(
              text = "• ${video.formattedViews} • ${video.uploadDate}",
              style = MaterialTheme.typography.bodySmall.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                fontSize = 11.sp
              ),
              maxLines = 1
            )
          }
        }

        // Context Menu
        Box {
          IconButton(
            onClick = { menuExpanded = true },
            modifier = Modifier.size(32.dp).testTag("video_options_${video.id}")
          ) {
            Icon(
              imageVector = Icons.Default.MoreVert,
              contentDescription = "More options",
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(18.dp)
            )
          }

          DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false },
            modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
          ) {
            DropdownMenuItem(
              text = { Text("Save to Watch Later") },
              leadingIcon = { Icon(Icons.Default.WatchLater, contentDescription = null) },
              onClick = {
                menuExpanded = false
                onSaveToWatchLater()
              }
            )
            DropdownMenuItem(
              text = { Text("Add to Queue") },
              leadingIcon = { Icon(Icons.Default.Queue, contentDescription = null) },
              onClick = {
                menuExpanded = false
                onAddToQueue()
              }
            )
            DropdownMenuItem(
              text = { Text("Share Video") },
              leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
              onClick = {
                menuExpanded = false
                onShare()
              }
            )
          }
        }
      }
    }
  }
}
