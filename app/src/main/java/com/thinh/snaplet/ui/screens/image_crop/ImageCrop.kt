package com.thinh.snaplet.ui.screens.image_crop

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.thinh.snaplet.ui.components.BaseText
import com.thinh.snaplet.ui.theme.Typography

val FRAME_SIZE = 400.dp

@Composable
fun ImageCrop(
    onCropDone: (Uri) -> Unit = {},
    onBack: () -> Unit = {},
    cropImageViewModel: ImageCropViewModel = hiltViewModel(),
) {
    val uiState by cropImageViewModel.uiState.collectAsStateWithLifecycle()
    val imageUri: Uri? = uiState.imageUri?.toUri()

    val context = LocalContext.current
    val density = LocalDensity.current
    val framePx = with(density) { FRAME_SIZE.toPx() }

    var rotationDeg by remember { mutableIntStateOf(0) }
    var isFlippedH by remember { mutableStateOf(false) }
    var isFlippedV by remember { mutableStateOf(false) }
    var cropTrigger by remember { mutableIntStateOf(0) }

    BackHandler(enabled = true, onBack = {})

    LaunchedEffect(uiState.croppedUri) {
        uiState.croppedUri?.let { uri ->
            onCropDone(uri)
        }
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
                        frameSize = FRAME_SIZE,
                        rotationDeg = rotationDeg,
                        isFlippedH = isFlippedH,
                        isFlippedV = isFlippedV,
                        cropTrigger = cropTrigger,
                        onCropReady = { w, h, scale, frameTop ->
                            cropImageViewModel.cropImage(
                                context = context,
                                uri = imageUri,
                                displayImageW = w,
                                displayImageH = h,
                                displayScale = scale,
                                frameTop = frameTop,
                                framePx = framePx,
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
    frameSize: Dp = FRAME_SIZE,
    rotationDeg: Int = 0,
    isFlippedH: Boolean = false,
    isFlippedV: Boolean = false,
    cropTrigger: Int = 0,
    onCropReady: (w: Int, h: Int, scale: Float, frameTop: Float) -> Unit = { _, _, _, _ -> },
) {
    val context = LocalContext.current
    val density = LocalDensity.current

    var imageWidthPx by remember { mutableIntStateOf(0) }
    var imageHeightPx by remember { mutableIntStateOf(0) }

    val framePx = with(density) { frameSize.toPx() }

    var frameOffsetY by remember { mutableFloatStateOf(0f) }
    var imageScale by remember { mutableFloatStateOf(1f) }

    val isSwapped = rotationDeg == 90 || rotationDeg == 270
    val effectiveW = if (isSwapped) imageHeightPx else imageWidthPx
    val effectiveH = if (isSwapped) imageWidthPx else imageHeightPx

    fun computeMinScale(): Float {
        if (effectiveH == 0) return 1f
        return maxOf(framePx / effectiveH, 1f)
    }

    fun clampFrame(offset: Float): Float {
        if (effectiveH == 0) return 0f
        val halfFrame = framePx / 2f
        val halfImg = effectiveH / 2f
        if (halfImg <= halfFrame) return 0f
        return offset.coerceIn(-(halfImg - halfFrame), halfImg - halfFrame)
    }

    LaunchedEffect(rotationDeg) {
        val minScale = computeMinScale()
        if (imageScale < minScale) imageScale = minScale
        frameOffsetY = 0f
    }

    val frameTop = effectiveH / 2f - framePx / 2f + frameOffsetY

    LaunchedEffect(cropTrigger) {
        if (cropTrigger > 0 && imageWidthPx > 0 && imageHeightPx > 0) {
            onCropReady(imageWidthPx, imageHeightPx, imageScale, frameTop)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .clip(RectangleShape)
            .pointerInput(rotationDeg) {
                detectTransformGestures { _, pan, zoom, _ ->
                    if (zoom != 1f) {
                        val minScale = computeMinScale()
                        imageScale = (imageScale * zoom).coerceIn(minScale, minScale * 5f)
                    } else {
                        frameOffsetY = clampFrame(frameOffsetY + pan.y)
                    }
                }
            }
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context).data(uri).crossfade(true).build(),
            contentDescription = null,
            contentScale = ContentScale.FillWidth,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
                .onGloballyPositioned { coords ->
                    val newH = coords.size.height
                    val newW = coords.size.width
                    if (newH != imageHeightPx || newW != imageWidthPx) {
                        imageWidthPx = newW
                        imageHeightPx = newH
                        val minScale = computeMinScale()
                        if (imageScale < minScale) {
                            imageScale = minScale
                            frameOffsetY = 0f
                        }
                    }
                }
                .graphicsLayer {
                    scaleX = imageScale * if (isFlippedH) -1f else 1f
                    scaleY = imageScale * if (isFlippedV) -1f else 1f
                    rotationZ = rotationDeg.toFloat()
                    transformOrigin = TransformOrigin(0.5f, 0.5f)
                }
        )

        if (imageHeightPx > 0) {
            Canvas(
                modifier = Modifier
                    .width(with(density) { effectiveW.toDp() })
                    .height(with(density) { effectiveH.toDp() })
                    .align(Alignment.Center)
            ) {
                drawDimRects(frameTop, framePx)
                drawGrid(frameTop, framePx)
                drawCornerHandles(frameTop, framePx)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
//  Drawing
// ─────────────────────────────────────────────────────────────

private fun DrawScope.drawDimRects(frameTop: Float, frameH: Float) {
    val dim = Color(0x99000000)
    val frameBottom = frameTop + frameH
    if (frameTop > 0f)
        drawRect(dim, topLeft = Offset.Zero, size = Size(size.width, frameTop))
    if (frameBottom < size.height)
        drawRect(
            dim,
            topLeft = Offset(0f, frameBottom),
            size = Size(size.width, size.height - frameBottom)
        )
}

private fun DrawScope.drawGrid(frameTop: Float, frameH: Float) {
    clipRect(0f, frameTop, size.width, frameTop + frameH) {
        val color = Color.White.copy(alpha = .55f)
        val stroke = 2.dp.toPx()
        val frameBottom = frameTop + frameH
        val right = size.width

        drawLine(color, Offset(0f, frameTop), Offset(right, frameTop), strokeWidth = stroke)
        drawLine(color, Offset(0f, frameBottom), Offset(right, frameBottom), strokeWidth = stroke)
        drawLine(color, Offset(0f, frameTop), Offset(0f, frameBottom), strokeWidth = stroke)
        drawLine(color, Offset(right, frameTop), Offset(right, frameBottom), strokeWidth = stroke)

        for (i in 1..2) {
            val x = right * i / 3f
            val y = frameTop + frameH * i / 3f
            drawLine(color, Offset(x, frameTop), Offset(x, frameBottom), strokeWidth = stroke)
            drawLine(color, Offset(0f, y), Offset(right, y), strokeWidth = stroke)
        }
    }
}

private fun DrawScope.drawCornerHandles(frameTop: Float, frameH: Float) {
    val color = Color.White
    val stroke = 3.dp.toPx()
    val len = 16.dp.toPx()
    val inset = 2.dp.toPx()
    val frameBottom = frameTop + frameH
    val right = size.width

    val l = inset
    val t = frameTop + inset
    val b = frameBottom - inset
    val r = right - inset

    drawLine(color, Offset(l, t), Offset(l + len, t), strokeWidth = stroke)
    drawLine(color, Offset(l, t), Offset(l, t + len), strokeWidth = stroke)
    drawLine(color, Offset(r, t), Offset(r - len, t), strokeWidth = stroke)
    drawLine(color, Offset(r, t), Offset(r, t + len), strokeWidth = stroke)
    drawLine(color, Offset(l, b), Offset(l + len, b), strokeWidth = stroke)
    drawLine(color, Offset(l, b), Offset(l, b - len), strokeWidth = stroke)
    drawLine(color, Offset(r, b), Offset(r - len, b), strokeWidth = stroke)
    drawLine(color, Offset(r, b), Offset(r, b - len), strokeWidth = stroke)
}