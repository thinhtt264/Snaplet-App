package com.thinh.snaplet.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale

/**
 * A reusable animated button wrapper that provides press animation effect.
 * 
 * @param onClick Lambda to be invoked when the button is clicked
 * @param modifier Modifier to be applied to the button
 * @param enabled Whether the button is enabled for clicks
 * @param interactionSource Optional interaction source for handling press states
 * @param scaleOnPress The scale factor to apply when button is pressed (default: 0.97f)
 * @param animationDuration Duration of the scale animation in milliseconds (default: 150)
 * @param content The composable content to display inside the button
 */
@Composable
fun AnimatedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
    scaleOnPress: Float = 0.97f,
    animationDuration: Int = 150,
    content: @Composable () -> Unit
) {
    val internalInteractionSource = remember { MutableInteractionSource() }
    val finalInteractionSource = interactionSource ?: internalInteractionSource
    val isPressed by finalInteractionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) scaleOnPress else 1f,
        animationSpec = tween(durationMillis = animationDuration),
        label = "animated_button_scale"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .clickable(
                enabled = enabled,
                onClick = onClick,
                indication = null,
                interactionSource = finalInteractionSource
            )
    ) {
        content()
    }
}

