package com.thinh.snaplet.ui.screens.chat.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlin.math.sin

private val BubbleTheirs = Color(0xFF1E2020)
private val BUBBLE_CORNER = 16.dp
private val BUBBLE_CORNER_SMALL = 4.dp

@Composable
fun TypingIndicator(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "typing")

    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "wave",
    )

    Row(
        modifier = modifier
            .clip(
                RoundedCornerShape(
                    topStart = BUBBLE_CORNER_SMALL,
                    topEnd = BUBBLE_CORNER,
                    bottomStart = BUBBLE_CORNER,
                    bottomEnd = BUBBLE_CORNER,
                )
            )
            .background(BubbleTheirs)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val bounceHeight = 4f
        val phaseOffset = 1f / 3f

        repeat(3) { index ->
            // Continuous sine wave — dot i samples the wave at its own phase offset
            val phase = (progress - index * phaseOffset) * 2f * Math.PI.toFloat()
            val sineValue = -sin(phase)            // negative so positive = up
            val offsetY = sineValue * bounceHeight

            Box(
                modifier = Modifier
                    .size(6.dp)
                    .graphicsLayer {
                        translationY = offsetY
                        scaleX = 1f + if (sineValue > 0f) sineValue * 0.08f else 0f
                        scaleY = scaleX
                    }
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.5f))
            )
        }
    }
}