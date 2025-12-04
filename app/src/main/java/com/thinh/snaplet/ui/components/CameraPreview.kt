package com.thinh.snaplet.ui.components

import android.util.Rational
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import androidx.camera.core.ViewPort
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.thinh.snaplet.utils.Logger

@Composable
fun CameraPreview(
    modifier: Modifier = Modifier, onImageCaptureReady: (ImageCapture) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor = remember { ContextCompat.getMainExecutor(context) }

    Box(modifier) {
        AndroidView(
            factory = { ctx ->
                Logger.d("Creating PreviewView...")
                val previewView = PreviewView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                }

                // Wait for view to be measured before binding camera
                previewView.post {
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                    cameraProviderFuture.addListener({
                        try {
                            Logger.d("Getting CameraProvider...")
                            val cameraProvider = cameraProviderFuture.get()

                            val width = previewView.width
                            val height = previewView.height
                            Logger.d("PreviewView size: ${width}x${height}")

                            val aspectRatio = Rational(width, height)
                            val rotation = previewView.display.rotation
                            val preview = Preview.Builder().build().also {
                                    it.surfaceProvider = previewView.surfaceProvider
                                }
                            val imageCapture = ImageCapture.Builder()
                                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build()

                            // Notify parent that ImageCapture is ready
                            onImageCaptureReady(imageCapture)
                            Logger.d("ImageCapture ready callback sent")

                            val viewPort = ViewPort.Builder(aspectRatio, rotation).build()

                            val useCaseGroup =
                                UseCaseGroup.Builder().addUseCase(preview).addUseCase(imageCapture)
                                    .setViewPort(viewPort).build()

                            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner, cameraSelector, useCaseGroup
                            )
                        } catch (e: Exception) {
                            Logger.e("Camera binding failed: ${e.message}", e)
                        }
                    }, executor)
                }
                previewView
            }, modifier = Modifier.fillMaxSize()
        )
    }
}

