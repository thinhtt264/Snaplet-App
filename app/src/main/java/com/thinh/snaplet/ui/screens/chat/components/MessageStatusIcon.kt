package com.thinh.snaplet.ui.screens.chat.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.thinh.snaplet.ui.theme.MotionTokens

@Composable
fun MessageStatusIcon(
    isPending: Boolean,
    showError: Boolean,
    showSeenTick: Boolean,
    iconSize: Dp = 14.dp,
    defaultTint: Color = Color(0xFF0D0D0D),
) {
    val infiniteTransition = rememberInfiniteTransition(label = "loading")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = MotionTokens.LoadingRotation, easing = LinearEasing)
        ),
        label = "rotation",
    )

    when {
        isPending -> Icon(
            imageVector = Icons.Filled.Refresh,
            contentDescription = null,
            modifier = Modifier
                .size(iconSize)
                .rotate(rotation),
            tint = defaultTint,
        )

        showError -> Icon(
            imageVector = Icons.Filled.Error,
            contentDescription = null,
            modifier = Modifier.size(iconSize),
            tint = MaterialTheme.colorScheme.error,
        )

        showSeenTick -> Icon(
            imageVector = Icons.Filled.DoneAll,
            contentDescription = null,
            modifier = Modifier.size(iconSize),
            tint = defaultTint,
        )

        else -> Icon(
            imageVector = Icons.Filled.Done,
            contentDescription = null,
            modifier = Modifier.size(iconSize),
            tint = defaultTint,
        )
    }
}
