package com.thinh.snaplet.ui.components

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun EmojiFloatCanvas(
    controller: EmojiFloatController,
    modifier: Modifier = Modifier,
) {
    val particles by controller.particles.collectAsStateWithLifecycle()
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current
    val horizontalPaddingPx = remember(density) { with(density) { 8.dp.toPx() } }

    LaunchedEffect(canvasSize) {
        if (canvasSize.width > 0 && canvasSize.height > 0) {
            controller.onCanvasMeasured(
                canvasSize.width.toFloat(),
                canvasSize.height.toFloat(),
                horizontalPaddingPx,
            )
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            withFrameMillis { }
            controller.tick()
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { canvasSize = it }
    ) {
        particles.forEach { p ->
            drawContext.canvas.nativeCanvas.drawText(
                p.emoji,
                p.x,
                p.y,
                Paint().apply {
                    textSize = p.size
                    alpha = (p.alpha * 255).toInt()
                },
            )
        }
    }
}
