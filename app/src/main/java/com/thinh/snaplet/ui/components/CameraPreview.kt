package com.thinh.snaplet.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.util.Rational
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import androidx.camera.core.ViewPort
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.thinh.snaplet.utils.Logger
import java.util.concurrent.Executor

private const val STREAMING_READY_DELAY = 100L
private const val FADE_IN_DURATION = 300
private const val FADE_OUT_DURATION = 400

@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    shouldBindCamera: Boolean = true,
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

    Box(modifier) {
        if (shouldBindCamera) {
            CameraPreviewView(
                context = context,
                lifecycleOwner = lifecycleOwner,
                onPreviewViewCreated = { currentPreviewView = it },
                onImageCaptureReady = onImageCaptureReady,
                onStreamingStateChanged = { isStreaming ->
                    isCameraStreamingReady = isStreaming
                }
            )
        }

        CameraPlaceholderOverlay(
            isVisible = !isCameraStreamingReady,
            snapshot = placeholderBitmap
        )
    }
}

@Composable
private fun CameraPreviewView(
    context: Context,
    lifecycleOwner: LifecycleOwner,
    onPreviewViewCreated: (PreviewView) -> Unit,
    onImageCaptureReady: (ImageCapture) -> Unit,
    onStreamingStateChanged: (Boolean) -> Unit
) {
    val executor = remember { ContextCompat.getMainExecutor(context) }

    AndroidView(
        factory = { ctx ->
            createPreviewView(ctx).also { previewView ->
                onPreviewViewCreated(previewView)
                setupCamera(
                    context = ctx,
                    previewView = previewView,
                    lifecycleOwner = lifecycleOwner,
                    executor = executor,
                    onImageCaptureReady = onImageCaptureReady,
                    onStreamingStateChanged = onStreamingStateChanged
                )
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun CameraPlaceholderOverlay(
    isVisible: Boolean,
    snapshot: Bitmap?
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = tween(FADE_IN_DURATION)),
        exit = fadeOut(animationSpec = tween(FADE_OUT_DURATION))
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
    Logger.d("Creating PreviewView...")
    return PreviewView(context).apply {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        scaleType = PreviewView.ScaleType.FILL_CENTER
        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
    }
}

private fun setupCamera(
    context: Context,
    previewView: PreviewView,
    lifecycleOwner: LifecycleOwner,
    executor: Executor,
    onImageCaptureReady: (ImageCapture) -> Unit,
    onStreamingStateChanged: (Boolean) -> Unit
) {
    previewView.post {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()
                bindCameraUseCases(
                    cameraProvider = cameraProvider,
                    previewView = previewView,
                    lifecycleOwner = lifecycleOwner,
                    onImageCaptureReady = onImageCaptureReady,
                    onStreamingStateChanged = onStreamingStateChanged
                )
            } catch (e: Exception) {
                Logger.e("Camera binding failed: ${e.message}", e)
            }
        }, executor)
    }
}

private fun bindCameraUseCases(
    cameraProvider: ProcessCameraProvider,
    previewView: PreviewView,
    lifecycleOwner: LifecycleOwner,
    onImageCaptureReady: (ImageCapture) -> Unit,
    onStreamingStateChanged: (Boolean) -> Unit
) {
    Logger.d("Binding camera use cases...")

    val width = previewView.width
    val height = previewView.height
    Logger.d("PreviewView size: ${width}x${height}")

    // Build preview use case
    val preview = Preview.Builder()
        .build()
        .also { it.surfaceProvider = previewView.surfaceProvider }

    // Build image capture use case
    val imageCapture = ImageCapture.Builder()
        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
        .build()

    onImageCaptureReady(imageCapture)
    Logger.d("ImageCapture ready callback sent")

    // Create viewport and use case group
    val aspectRatio = Rational(width, height)
    val rotation = previewView.display.rotation
    val viewPort = ViewPort.Builder(aspectRatio, rotation).build()

    val useCaseGroup = UseCaseGroup.Builder()
        .addUseCase(preview)
        .addUseCase(imageCapture)
        .setViewPort(viewPort)
        .build()

    // Bind to lifecycle
    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

    cameraProvider.unbindAll()
    cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, useCaseGroup)
    Logger.d("Camera successfully bound to lifecycle")

    // Monitor streaming state
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
                        Logger.d("🎥 Camera streaming ready")
                    }, STREAMING_READY_DELAY)
                }
            }

            else -> {
                Logger.d("📹 Camera stream state: $streamState")
            }
        }
    }
}

private fun captureSnapshot(previewView: PreviewView): Bitmap? {
    return previewView.bitmap?.also { bitmap ->
        Logger.d("📸 Captured bitmap: ${bitmap.width}x${bitmap.height}")
    } ?: run {
        Logger.e("❌ Failed to capture bitmap - bitmap is null")
        null
    }
}

