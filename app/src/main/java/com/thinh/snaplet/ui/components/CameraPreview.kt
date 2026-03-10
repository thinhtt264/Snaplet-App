package com.thinh.snaplet.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.util.Rational
import android.view.Surface
import android.view.ViewGroup
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import androidx.camera.core.UseCaseGroup
import androidx.camera.core.ViewPort
import androidx.camera.core.ZoomState
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.thinh.snaplet.ui.theme.MotionTokens
import com.thinh.snaplet.utils.Logger
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private const val STREAMING_READY_DELAY = 100L

@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    shouldBindCamera: Boolean = true,
    lensFacing: Int = CameraSelector.LENS_FACING_FRONT,
    placeholderBitmap: Bitmap?,
    onImageCaptureReady: (ImageCapture) -> Unit,
    onSnapshotHandlerReady: (() -> Bitmap?) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var currentPreviewView by remember { mutableStateOf<PreviewView?>(null) }
    var isCameraStreamingReady by remember { mutableStateOf(false) }

    LaunchedEffect(shouldBindCamera) {
        if (!shouldBindCamera) {
            isCameraStreamingReady = false
        }
    }

    LaunchedEffect(currentPreviewView) {
        currentPreviewView?.let { previewView ->
            onSnapshotHandlerReady { captureSnapshot(previewView) }
        }
    }

    var boundCamera by remember { mutableStateOf<Camera?>(null) }

    Box(modifier) {
        if (shouldBindCamera) {
            CameraPreviewView(
                context = context,
                lifecycleOwner = lifecycleOwner,
                lensFacing = lensFacing,
                onPreviewViewCreated = { currentPreviewView = it },
                onImageCaptureReady = onImageCaptureReady,
                onStreamingStateChanged = { isStreaming ->
                    isCameraStreamingReady = isStreaming
                },
                onCameraBound = { boundCamera = it }
            )
        }
        LaunchedEffect(shouldBindCamera) {
            if (!shouldBindCamera) boundCamera = null
        }

        CameraPlaceholderOverlay(
            isVisible = !isCameraStreamingReady,
            snapshot = placeholderBitmap
        )

        boundCamera?.let { camera ->
            CameraGestureLayer(
                camera = camera,
                previewView = currentPreviewView,
                lifecycleOwner = lifecycleOwner
            )
        }
    }
}

@Composable
private fun CameraPreviewView(
    context: Context,
    lifecycleOwner: LifecycleOwner,
    lensFacing: Int,
    onPreviewViewCreated: (PreviewView) -> Unit,
    onImageCaptureReady: (ImageCapture) -> Unit,
    onStreamingStateChanged: (Boolean) -> Unit,
    onCameraBound: (Camera?) -> Unit
) {
    val executor = remember { ContextCompat.getMainExecutor(context) }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var previewView by remember { mutableStateOf<PreviewView?>(null) }

    // Initialize CameraProvider
    LaunchedEffect(context) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
        }, executor)
    }

    // Bind/Re-bind camera when dependencies change
    LaunchedEffect(cameraProvider, previewView, lensFacing) {
        val provider = cameraProvider
        val view = previewView
        if (provider != null && view != null) {
            bindCameraUseCases(
                cameraProvider = provider,
                previewView = view,
                lensFacing = lensFacing,
                lifecycleOwner = lifecycleOwner,
                onImageCaptureReady = onImageCaptureReady,
                onStreamingStateChanged = onStreamingStateChanged,
                onCameraBound = onCameraBound
            )
        } else {
            onCameraBound(null)
        }
    }

    AndroidView(
        factory = { ctx ->
            createPreviewView(ctx).also {
                previewView = it
                onPreviewViewCreated(it)
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun CameraGestureLayer(
    camera: Camera,
    previewView: PreviewView?,
    lifecycleOwner: LifecycleOwner
) {
//    var zoomRatio by remember { mutableFloatStateOf(1f) }
    var minZoom by remember { mutableFloatStateOf(1f) }
    var maxZoom by remember { mutableFloatStateOf(1f) }
    var lastAppliedRatio by remember { mutableFloatStateOf(1f) }
    var focusIndicatorOffset by remember { mutableStateOf<Offset?>(null) }

    DisposableEffect(camera) {
        val observer = Observer<ZoomState?> { state ->
            state?.let {
//                zoomRatio = it.zoomRatio
                minZoom = it.minZoomRatio
                maxZoom = it.maxZoomRatio
                lastAppliedRatio = it.zoomRatio
            }
        }
        camera.cameraInfo.zoomState.observe(lifecycleOwner, observer)
        onDispose {
            camera.cameraInfo.zoomState.removeObserver(observer)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(camera, previewView) {
                detectTapGestures { offset ->
                    val view = previewView
                    if (view != null && view.width > 0 && view.height > 0) {
                        val factory = SurfaceOrientedMeteringPointFactory(
                            view.width.toFloat(),
                            view.height.toFloat()
                        )
                        val point = factory.createPoint(offset.x, offset.y)
                        val action = FocusMeteringAction.Builder(point).build()
                        camera.cameraControl.startFocusAndMetering(action)
                        focusIndicatorOffset = offset
                    }
                }
            }
            .pointerInput(camera) {
                detectTransformGestures { _, _, zoom, _ ->
                    val newRatio = (lastAppliedRatio * zoom).coerceIn(minZoom, maxZoom)
                    lastAppliedRatio = newRatio
                    camera.cameraControl.setZoomRatio(newRatio)
                }
            }
    ) {
        focusIndicatorOffset?.let { offset ->
            LaunchedEffect(offset) {
                delay(1000)
                focusIndicatorOffset = null
            }
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(animationSpec = tween(150)),
                exit = fadeOut(animationSpec = tween(300)),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset { IntOffset(offset.x.roundToInt(), offset.y.roundToInt()) }
                    .offset((-24).dp, (-24).dp)
            ) {
                Canvas(modifier = Modifier.size(48.dp)) {
                    drawCircle(
                        color = Color.White,
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
            }
        }
    }
}

@Composable
private fun CameraPlaceholderOverlay(
    isVisible: Boolean,
    snapshot: Bitmap?
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = tween(MotionTokens.Slow)),
        exit = fadeOut(animationSpec = tween(MotionTokens.Slow))
    ) {
        snapshot?.let { bitmap ->
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Camera preview placeholder",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
    }
}

private fun createPreviewView(context: Context): PreviewView {
    return PreviewView(context).apply {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        scaleType = PreviewView.ScaleType.FILL_CENTER
        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
    }
}

private fun bindCameraUseCases(
    cameraProvider: ProcessCameraProvider,
    previewView: PreviewView,
    lensFacing: Int,
    lifecycleOwner: LifecycleOwner,
    onImageCaptureReady: (ImageCapture) -> Unit,
    onStreamingStateChanged: (Boolean) -> Unit,
    onCameraBound: (Camera?) -> Unit
) {
    val width = previewView.width
    val height = previewView.height

    // If width or height is 0, we might need to wait for layout
    if (width <= 0 || height <= 0) {
        previewView.post {
            bindCameraUseCases(
                cameraProvider,
                previewView,
                lensFacing,
                lifecycleOwner,
                onImageCaptureReady,
                onStreamingStateChanged,
                onCameraBound
            )
        }
        return
    }

    val resolutionSelector = ResolutionSelector.Builder()
        .setResolutionStrategy(ResolutionStrategy.HIGHEST_AVAILABLE_STRATEGY)
        .build()

    val preview = Preview.Builder()
        .setResolutionSelector(resolutionSelector)
        .build()
        .also { it.surfaceProvider = previewView.surfaceProvider }

    val aspectRatio = Rational(width, height)
    val rotation = previewView.display?.rotation ?: Surface.ROTATION_0

    val imageCapture = ImageCapture.Builder()
        .setResolutionSelector(resolutionSelector)
        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
        .setTargetRotation(rotation)
        .build()

    onImageCaptureReady(imageCapture)

    val viewPort = ViewPort.Builder(aspectRatio, rotation).build()

    val useCaseGroup = UseCaseGroup.Builder()
        .addUseCase(preview)
        .addUseCase(imageCapture)
        .setViewPort(viewPort)
        .build()

    val cameraSelector = CameraSelector.Builder()
        .requireLensFacing(lensFacing)
        .build()

    try {
        onCameraBound(null)
        cameraProvider.unbindAll()
        val camera = cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, useCaseGroup)
        onCameraBound(camera)
    } catch (e: Exception) {
        Logger.e("Camera binding failed: ${e.message}", e)
        onCameraBound(null)
    }

    observeStreamingState(previewView, lifecycleOwner, onStreamingStateChanged)
}

private fun observeStreamingState(
    previewView: PreviewView,
    lifecycleOwner: LifecycleOwner,
    onStreamingStateChanged: (Boolean) -> Unit
) {
    var isStreamingReady = false

    previewView.previewStreamState.observe(lifecycleOwner) { streamState ->
        when (streamState) {
            PreviewView.StreamState.STREAMING -> {
                if (!isStreamingReady) {
                    previewView.postDelayed({
                        isStreamingReady = true
                        onStreamingStateChanged(true)
                    }, STREAMING_READY_DELAY)
                }
            }

            else -> {
                // Logger.d("📹 Camera stream state: $streamState")
                if (isStreamingReady) {
                    isStreamingReady = false
                    onStreamingStateChanged(false)
                }
            }
        }
    }
}

private fun captureSnapshot(previewView: PreviewView): Bitmap? {
    return previewView.bitmap ?: run {
        Logger.e("❌ Failed to capture bitmap - bitmap is null")
        null
    }
}