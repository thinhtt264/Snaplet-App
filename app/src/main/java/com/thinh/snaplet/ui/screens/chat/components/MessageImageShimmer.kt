package com.thinh.snaplet.ui.screens.chat.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

@Composable
fun MessageImageShimmer(
    modifier: Modifier = Modifier,
    baseColor: Color,
) {
    val transition = rememberInfiniteTransition(label = "msg_img_shimmer")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmer_progress",
    )

    val highlight = if (baseColor.luminance() < 0.5f) {
        Color.White.copy(alpha = 0.22f)
    } else {
        Color.Black.copy(alpha = 0.10f)
    }

    Box(
        modifier = modifier.drawBehind {
            val shimmerBandWidth = size.width * 0.55f
            val offsetX = progress * (size.width + shimmerBandWidth) - shimmerBandWidth

            val brush = Brush.linearGradient(
                colors = listOf(baseColor, highlight, baseColor),
                start = Offset(offsetX, 0f),
                end = Offset(offsetX + shimmerBandWidth, 0f),
            )
            drawRect(brush = brush, size = size)
        }
    )
}
