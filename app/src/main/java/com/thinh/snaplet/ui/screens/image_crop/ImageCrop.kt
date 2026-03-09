package com.thinh.snaplet.ui.screens.image_crop

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.RotateRight
import androidx.compose.material.icons.outlined.Flip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.thinh.snaplet.ui.components.BaseText
import com.thinh.snaplet.ui.components.image.AsyncImage
import com.thinh.snaplet.ui.components.image.ImageSize
import com.thinh.snaplet.ui.theme.Typography
import kotlin.math.min
import kotlin.math.sqrt

private val MAX_FRAME_HEIGHT = 400.dp
private val HANDLE_ZONE = 44.dp
private const val MIN_FRAME_RATIO = 0.2f

private enum class DragHandle {
    TopLeft, TopRight, BottomLeft, BottomRight, None
}

@Composable
fun ImageCrop(
    onCropDone: (Uri) -> Unit = {},
    onBack: () -> Unit = {},
    cropImageViewModel: ImageCropViewModel = hiltViewModel(),
) {
    val uiState by cropImageViewModel.uiState.collectAsStateWithLifecycle()
    val imageUri: Uri? = uiState.imageUri?.toUri()
    val context = LocalContext.current

    var rotationDeg by remember { mutableIntStateOf(0) }
    var isFlippedH by remember { mutableStateOf(false) }
    var isFlippedV by remember { mutableStateOf(false) }
    var cropTrigger by remember { mutableIntStateOf(0) }

    BackHandler(enabled = true, onBack = {})

    LaunchedEffect(uiState.croppedUri) {
        uiState.croppedUri?.let { uri -> onCropDone(uri) }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        ImageCropHeader(
            isCropping = uiState.isCropping,
            isFlippedH = isFlippedH,
            isFlippedV = isFlippedV,
            onBack = onBack,
            onRotate = { rotationDeg = (rotationDeg + 90) % 360 },
            onFlipH = { isFlippedH = !isFlippedH },
            onFlipV = { isFlippedV = !isFlippedV },
            onCrop = { cropTrigger++ }
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            if (imageUri != null) {
                key(imageUri) {
                    ImageCropper(
                        uri = imageUri,
                        rotationDeg = rotationDeg,
                        isFlippedH = isFlippedH,
                        isFlippedV = isFlippedV,
                        cropTrigger = cropTrigger,
                        onCropReady = { baseW, baseH, frameLeft, frameTop, fW, fH ->
                            cropImageViewModel.cropImage(
                                context = context,
                                uri = imageUri,
                                displayImageW = baseW,
                                displayImageH = baseH,
                                frameLeft = frameLeft,
                                frameTop = frameTop,
                                frameW = fW,
                                frameH = fH,
                                rotationDeg = rotationDeg,
                                isFlippedH = isFlippedH,
                                isFlippedV = isFlippedV,
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ImageCropHeader(
    isCropping: Boolean,
    isFlippedH: Boolean,
    isFlippedV: Boolean,
    onBack: () -> Unit,
    onRotate: () -> Unit,
    onFlipH: () -> Unit,
    onFlipV: () -> Unit,
    onCrop: () -> Unit,
) {
    val accentColor = MaterialTheme.colorScheme.primary
    var showFlipMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.White
            )
        }

        Spacer(Modifier.weight(1f))

        IconButton(onClick = onRotate) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.RotateRight,
                contentDescription = "Rotate",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }

        Box {
            IconButton(onClick = { showFlipMenu = true }) {
                Icon(
                    imageVector = Icons.Outlined.Flip,
                    contentDescription = "Flip",
                    tint = if (isFlippedH || isFlippedV) accentColor else Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            DropdownMenu(
                expanded = showFlipMenu,
                onDismissRequest = { showFlipMenu = false },
            ) {
                DropdownMenuItem(
                    text = {
                        Text(
                            "Lật theo chiều ngang",
                            color = if (isFlippedH) accentColor else Color.White
                        )
                    },
                    onClick = { onFlipH(); showFlipMenu = false }
                )
                DropdownMenuItem(
                    text = {
                        Text(
                            "Lật theo chiều dọc",
                            color = if (isFlippedV) accentColor else Color.White
                        )
                    },
                    onClick = { onFlipV(); showFlipMenu = false }
                )
            }
        }

        TextButton(onClick = onCrop, enabled = !isCropping) {
            if (isCropping) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White
                )
            } else {
                BaseText("CẮT", color = Color.White, typography = Typography.titleSmall)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
//  ImageCropper
// ─────────────────────────────────────────────────────────────

@Composable
fun ImageCropper(
    modifier: Modifier = Modifier,
    uri: Uri,
    maxFrameHeight: Dp = MAX_FRAME_HEIGHT,
    rotationDeg: Int = 0,
    isFlippedH: Boolean = false,
    isFlippedV: Boolean = false,
    cropTrigger: Int = 0,
    onCropReady: (
        baseW: Int, baseH: Int,
        frameLeft: Float, frameTop: Float,
        frameW: Float, frameH: Float
    ) -> Unit = { _, _, _, _, _, _ -> },
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val handleZonePx = with(density) { HANDLE_ZONE.toPx() }
    val maxFrameHPx = with(density) { maxFrameHeight.toPx() }

    var imageWidthPx by remember { mutableIntStateOf(0) }
    var imageHeightPx by remember { mutableIntStateOf(0) }

    val isSwapped = rotationDeg == 90 || rotationDeg == 270
    val baseW = if (isSwapped) imageHeightPx else imageWidthPx
    val baseH = if (isSwapped) imageWidthPx else imageHeightPx

    val maxFrameW = baseW.toFloat().coerceAtLeast(1f)
    val maxFrameH = min(baseH.toFloat(), maxFrameHPx).coerceAtLeast(1f)
    val minFrameW = (MIN_FRAME_RATIO * maxFrameW).coerceAtLeast(1f)
    val minFrameH = (MIN_FRAME_RATIO * maxFrameH).coerceAtLeast(1f)

    var frameW by remember { mutableFloatStateOf(0f) }
    var frameH by remember { mutableFloatStateOf(0f) }
    var frameOffsetX by remember { mutableFloatStateOf(0f) }
    var frameOffsetY by remember { mutableFloatStateOf(0f) }

    fun clampOffsetX(): Float {
        val slack = (baseW - frameW) / 2f
        return if (slack <= 0f) 0f else frameOffsetX.coerceIn(-slack, slack)
    }

    fun clampOffsetY(): Float {
        val slack = (baseH - frameH) / 2f
        return if (slack <= 0f) 0f else frameOffsetY.coerceIn(-slack, slack)
    }

    LaunchedEffect(baseW, baseH) {
        if (baseW > 0 && baseH > 0) {
            if (frameW <= 0 || frameH <= 0) {
                frameW = maxFrameW
                frameH = maxFrameH
            }
            frameW = frameW.coerceIn(minFrameW, maxFrameW)
            frameH = frameH.coerceIn(minFrameH, maxFrameH)
            frameOffsetX = clampOffsetX()
            frameOffsetY = clampOffsetY()
        }
    }

    LaunchedEffect(rotationDeg) {
        if (baseW > 0 && baseH > 0) {
            frameW = maxFrameW
            frameH = maxFrameH
            frameOffsetX = 0f
            frameOffsetY = 0f
        }
    }

    val frameLeft = (baseW - frameW) / 2f + frameOffsetX
    val frameTop = (baseH - frameH) / 2f + frameOffsetY
    val frameRect = Rect(frameLeft, frameTop, frameLeft + frameW, frameTop + frameH)

    fun detectDragHandle(pos: Offset): DragHandle {
        if (baseW <= 0 || baseH <= 0) return DragHandle.None
        val inL = pos.x in (frameRect.left - handleZonePx)..(frameRect.left + handleZonePx)
        val inR = pos.x in (frameRect.right - handleZonePx)..(frameRect.right + handleZonePx)
        val inT = pos.y in (frameRect.top - handleZonePx)..(frameRect.top + handleZonePx)
        val inB = pos.y in (frameRect.bottom - handleZonePx)..(frameRect.bottom + handleZonePx)
        if (inL && inT) return DragHandle.TopLeft
        if (inR && inT) return DragHandle.TopRight
        if (inL && inB) return DragHandle.BottomLeft
        if (inR && inB) return DragHandle.BottomRight
        return DragHandle.None
    }

    LaunchedEffect(cropTrigger) {
        if (cropTrigger > 0 && baseW > 0 && baseH > 0 && frameW > 0 && frameH > 0) {
            onCropReady(baseW, baseH, frameLeft, frameTop, frameW, frameH)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .clip(RectangleShape)
            .pointerInput(rotationDeg, baseW, baseH) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val handle = detectDragHandle(down.position)
                    var initialPinchSpan = 0f
                    var initialFrameW = frameW
                    var initialFrameH = frameH

                    do {
                        val event = awaitPointerEvent()
                        val current = event.changes.firstOrNull()
                        if (current != null && !current.pressed) {
                            current.consume()
                            break
                        }
                        when {
                            // Pinch: uniform frame resize
                            event.changes.size >= 2 -> {
                                val p0 = event.changes[0].position
                                val p1 = event.changes[1].position
                                val span = sqrt(
                                    (p1.x - p0.x) * (p1.x - p0.x) +
                                            (p1.y - p0.y) * (p1.y - p0.y)
                                )
                                if (initialPinchSpan <= 0f) {
                                    initialPinchSpan = span.coerceAtLeast(1f)
                                    initialFrameW = frameW
                                    initialFrameH = frameH
                                }
                                if (span > 0f) {
                                    val ratio = span / initialPinchSpan
                                    frameW = (initialFrameW * ratio).coerceIn(minFrameW, maxFrameW)
                                    frameH = (initialFrameH * ratio).coerceIn(minFrameH, maxFrameH)
                                    frameOffsetX = clampOffsetX()
                                    frameOffsetY = clampOffsetY()
                                }
                                event.changes.forEach { it.consume() }
                            }
                            // Corner drag: resize
                            handle != DragHandle.None -> {
                                val delta = current?.positionChange() ?: Offset.Zero
                                val (dw, dh) = when (handle) {
                                    DragHandle.TopLeft -> Pair(-delta.x, -delta.y)
                                    DragHandle.TopRight -> Pair(delta.x, -delta.y)
                                    DragHandle.BottomLeft -> Pair(-delta.x, delta.y)
                                    DragHandle.BottomRight -> Pair(delta.x, delta.y)
                                    else -> Pair(0f, 0f)
                                }
                                frameW = (frameW + dw).coerceIn(minFrameW, maxFrameW)
                                frameH = (frameH + dh).coerceIn(minFrameH, maxFrameH)
                                frameOffsetX = clampOffsetX()
                                frameOffsetY = clampOffsetY()
                                current?.consume()
                            }
                            // Single finger: scroll frame position
                            else -> {
                                val delta = current?.positionChange() ?: Offset.Zero
                                frameOffsetX += delta.x
                                frameOffsetY += delta.y
                                frameOffsetX = clampOffsetX()
                                frameOffsetY = clampOffsetY()
                                current?.consume()
                            }
                        }
                    } while (event.changes.any { it.pressed })
                }
            }
    ) {
        AsyncImage(
            imageUrl = uri.toString(),
            contentDescription = null,
            contentScale = ContentScale.FillWidth,
            resizeSize = ImageSize.Original,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
                .onGloballyPositioned { coords ->
                    val newW = coords.size.width
                    val newH = coords.size.height
                    if (newW != imageWidthPx || newH != imageHeightPx) {
                        imageWidthPx = newW
                        imageHeightPx = newH
                    }
                }
                .graphicsLayer {
                    scaleX = if (isFlippedH) -1f else 1f
                    scaleY = if (isFlippedV) -1f else 1f
                    rotationZ = rotationDeg.toFloat()
                    transformOrigin = TransformOrigin(0.5f, 0.5f)
                }
        )

        if (baseW > 0 && baseH > 0 && frameW > 0 && frameH > 0) {
            Canvas(
                modifier = Modifier
                    .width(with(density) { baseW.toDp() })
                    .height(with(density) { baseH.toDp() })
                    .align(Alignment.Center)
            ) {
                drawDimOverlay(frameRect)
                drawGrid(frameRect)
                drawCornerHandles(frameRect)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
//  Drawing
// ─────────────────────────────────────────────────────────────

private fun DrawScope.drawDimOverlay(frame: Rect) {
    val dim = Color(0x99000000)
    if (frame.top > 0f)
        drawRect(dim, Offset.Zero, Size(size.width, frame.top))
    if (frame.bottom < size.height)
        drawRect(dim, Offset(0f, frame.bottom), Size(size.width, size.height - frame.bottom))
    if (frame.left > 0f)
        drawRect(dim, Offset(0f, frame.top), Size(frame.left, frame.height))
    if (frame.right < size.width)
        drawRect(dim, Offset(frame.right, frame.top), Size(size.width - frame.right, frame.height))
}

private fun DrawScope.drawGrid(frame: Rect) {
    clipRect(frame.left, frame.top, frame.right, frame.bottom) {
        val color = Color.White.copy(alpha = .55f)
        val stroke = 2.dp.toPx()
        drawLine(
            color,
            Offset(frame.left, frame.top),
            Offset(frame.right, frame.top),
            strokeWidth = stroke
        )
        drawLine(
            color,
            Offset(frame.left, frame.bottom),
            Offset(frame.right, frame.bottom),
            strokeWidth = stroke
        )
        drawLine(
            color,
            Offset(frame.left, frame.top),
            Offset(frame.left, frame.bottom),
            strokeWidth = stroke
        )
        drawLine(
            color,
            Offset(frame.right, frame.top),
            Offset(frame.right, frame.bottom),
            strokeWidth = stroke
        )
        for (i in 1..2) {
            val x = frame.left + frame.width * i / 3f
            val y = frame.top + frame.height * i / 3f
            drawLine(color, Offset(x, frame.top), Offset(x, frame.bottom), strokeWidth = stroke)
            drawLine(color, Offset(frame.left, y), Offset(frame.right, y), strokeWidth = stroke)
        }
    }
}

private fun DrawScope.drawCornerHandles(frame: Rect) {
    val color = Color.White
    val stroke = 3.dp.toPx()
    val len = 16.dp.toPx()
    val inset = 2.dp.toPx()
    val l = frame.left + inset
    val t = frame.top + inset
    val r = frame.right - inset
    val b = frame.bottom - inset
    drawLine(color, Offset(l, t), Offset(l + len, t), strokeWidth = stroke)
    drawLine(color, Offset(l, t), Offset(l, t + len), strokeWidth = stroke)
    drawLine(color, Offset(r, t), Offset(r - len, t), strokeWidth = stroke)
    drawLine(color, Offset(r, t), Offset(r, t + len), strokeWidth = stroke)
    drawLine(color, Offset(l, b), Offset(l + len, b), strokeWidth = stroke)
    drawLine(color, Offset(l, b), Offset(l, b - len), strokeWidth = stroke)
    drawLine(color, Offset(r, b), Offset(r - len, b), strokeWidth = stroke)
    drawLine(color, Offset(r, b), Offset(r, b - len), strokeWidth = stroke)
}
