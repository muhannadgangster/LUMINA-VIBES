package com.example.ui.glass

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.model.GlassMode
import com.example.data.model.UserPreferences

val LocalUserPreferences = compositionLocalOf { UserPreferences() }

@Composable
fun Modifier.liquidGlassSurface(
  shape: Shape = RoundedCornerShape(16.dp),
  elevation: Dp = 8.dp,
  customIntensity: Float? = null,
  isInteractive: Boolean = false
): Modifier {
  val prefs = LocalUserPreferences.current
  val intensity = (customIntensity ?: prefs.glassIntensity).coerceIn(0.1f, 1.0f)
  val mode = prefs.glassMode

  if (mode == GlassMode.STANDARD) {
    // Clean standard Material surface fallback
    return this
      .shadow(elevation, shape, clip = false)
      .clip(shape)
      .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f))
      .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)), shape)
  }

  val infiniteTransition = rememberInfiniteTransition(label = "glassShimmer")
  val shimmerOffset by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 6000, easing = LinearEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "glassShimmerFloat"
  )

  val alphaBase = when (mode) {
    GlassMode.SUBTLE -> 0.75f * intensity
    GlassMode.ENHANCED -> 0.65f * intensity
    GlassMode.STANDARD -> 0.90f
  }

  // Deep obsidian crystal translucency
  val surfaceColorTop = Color(0xCC0E1526).copy(alpha = alphaBase)
  val surfaceColorBottom = Color(0xB3080D1A).copy(alpha = alphaBase * 0.9f)

  val borderAlpha = (0.22f * intensity).coerceIn(0.1f, 0.45f)
  val highlightAlpha = (0.35f * intensity).coerceIn(0.15f, 0.6f)

  val highlightShift = if (prefs.motionEffectsEnabled && !prefs.reducedMotion) {
    shimmerOffset * 0.3f
  } else 0f

  return this
    .shadow(
      elevation = if (mode == GlassMode.ENHANCED) elevation * 1.25f else elevation,
      shape = shape,
      ambientColor = Color(0x8000F0FF),
      spotColor = Color(0x408B5CF6)
    )
    .clip(shape)
    .background(
      brush = Brush.verticalGradient(
        listOf(surfaceColorTop, surfaceColorBottom)
      )
    )
    .drawBehind {
      // Specular highlight line along top edge
      val highlightWidth = size.width
      val startX = highlightWidth * highlightShift
      drawRect(
        brush = Brush.horizontalGradient(
          colors = listOf(
            Color.White.copy(alpha = highlightAlpha * 0.2f),
            Color(0xFF00F0FF).copy(alpha = highlightAlpha),
            Color.White.copy(alpha = highlightAlpha * 0.9f),
            Color(0xFF8B5CF6).copy(alpha = highlightAlpha * 0.6f),
            Color.Transparent
          ),
          startX = startX,
          endX = startX + highlightWidth
        ),
        topLeft = Offset.Zero,
        size = androidx.compose.ui.geometry.Size(size.width, 2.dp.toPx())
      )
    }
    .border(
      border = BorderStroke(
        width = 1.dp,
        brush = Brush.linearGradient(
          colors = listOf(
            Color.White.copy(alpha = borderAlpha * 1.5f),
            Color(0x6600F0FF).copy(alpha = borderAlpha),
            Color(0x448B5CF6).copy(alpha = borderAlpha * 0.8f),
            Color.White.copy(alpha = borderAlpha * 0.4f)
          ),
          start = Offset.Zero,
          end = Offset.Infinite
        )
      ),
      shape = shape
    )
}

@Composable
fun GlassCard(
  modifier: Modifier = Modifier,
  shape: Shape = RoundedCornerShape(18.dp),
  elevation: Dp = 6.dp,
  onClick: (() -> Unit)? = null,
  testTag: String = "glass_card",
  content: @Composable BoxScope.() -> Unit
) {
  val clickModifier = if (onClick != null) {
    Modifier
      .clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = ripple(color = Color(0xFF00F0FF)),
        onClick = onClick
      )
      .minimumInteractiveComponentSize()
  } else Modifier

  Box(
    modifier = modifier
      .testTag(testTag)
      .liquidGlassSurface(shape = shape, elevation = elevation, isInteractive = onClick != null)
      .then(clickModifier),
    content = content
  )
}

@Composable
fun GlassIconButton(
  icon: ImageVector,
  contentDescription: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  tint: Color = Color.White,
  size: Dp = 44.dp,
  testTag: String = "glass_icon_button"
) {
  Box(
    modifier = modifier
      .size(size)
      .testTag(testTag)
      .liquidGlassSurface(shape = CircleShape, elevation = 4.dp, isInteractive = true)
      .clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = ripple(bounded = true, color = Color(0xFF00F0FF)),
        onClick = onClick
      )
      .minimumInteractiveComponentSize(),
    contentAlignment = Alignment.Center
  ) {
    Icon(
      imageVector = icon,
      contentDescription = contentDescription,
      tint = tint,
      modifier = Modifier.size(size * 0.55f)
    )
  }
}

@Composable
fun GlassPill(
  modifier: Modifier = Modifier,
  shape: Shape = RoundedCornerShape(24.dp),
  content: @Composable BoxScope.() -> Unit
) {
  Box(
    modifier = modifier
      .liquidGlassSurface(shape = shape, elevation = 4.dp),
    contentAlignment = Alignment.Center,
    content = content
  )
}
