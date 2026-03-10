package com.thinh.snaplet.ui.components.image

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ===== Config sealed classes =====
sealed class LoadingStateConfig {
    data class Indicator(
        val backgroundColor: Color = Color.Unspecified,
        val indicatorSize: Dp = 32.dp,
        val strokeWidth: Dp = 3.dp,
        val color: Color = Color.Unspecified
    ) : LoadingStateConfig()

    data class Placeholder(
        val backgroundColor: Color = Color.Unspecified,
        val painter: Painter
    ) : LoadingStateConfig()

    data object None : LoadingStateConfig()
}

sealed class ErrorPlaceholderConfig {
    data class WithPainter(
        val painter: Painter,
        val size: Dp = 32.dp
    ) : ErrorPlaceholderConfig()

    data class WithIcon(
        val imageVector: ImageVector,
        val size: Dp = 32.dp,
        val tint: Color = Color.Unspecified
    ) : ErrorPlaceholderConfig()

    data class WithIconRes(
        val iconRes: Int,
        val size: Dp = 32.dp,
        val tint: Color = Color.Unspecified
    ) : ErrorPlaceholderConfig()

    data object None : ErrorPlaceholderConfig()
}

data class ErrorStateConfig(
    val backgroundColor: Color = Color.Unspecified,
    val placeholder: ErrorPlaceholderConfig = ErrorPlaceholderConfig.None
)