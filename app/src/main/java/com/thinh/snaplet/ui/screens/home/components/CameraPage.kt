package com.thinh.snaplet.ui.screens.home.components

import android.graphics.Bitmap
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.net.toUri
import com.thinh.snaplet.R
import com.thinh.snaplet.ui.components.BaseText
import com.thinh.snaplet.ui.components.CameraPreview
import com.thinh.snaplet.ui.components.image.AsyncImage
import com.thinh.snaplet.ui.components.image.ImageSize
import com.thinh.snaplet.ui.components.image.LoadingStateConfig
import com.thinh.snaplet.ui.screens.home.CameraActions
import com.thinh.snaplet.ui.screens.home.CameraState

private const val TOP_SPACE_RATIO = 0.12f

@Composable
fun CameraPage(
    cameraState: CameraState,
    currentCaption: String?,
    isUploading: Boolean,
    cameraActions: CameraActions,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(onTap = { focusManager.clearFocus() })
            }) {
        val screenHeight = maxHeight
        val density = LocalDensity.current
        val topPadding = screenHeight * TOP_SPACE_RATIO

        val imeHeightPx = WindowInsets.ime.getBottom(density).toFloat()

        val overlapPx = remember(imeHeightPx) {
            val mediaBottomPx =
                with(density) { (topPadding + MediaItemDimensions.MEDIA_HEIGHT).toPx() }
            val screenHeightPx = with(density) { screenHeight.toPx() }
            val availableSpacePx = screenHeightPx - imeHeightPx
            (mediaBottomPx - availableSpacePx).coerceAtLeast(0f)
        }
        val mediaOffsetPx by animateFloatAsState(
            targetValue = -overlapPx,
            animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
            label = "media_offset"
        )

        val actionOffsetPx = remember {
            with(density) { (screenHeight * (2.2f / 3f)).toPx() }
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = topPadding)
                .fillMaxWidth()
                .height(MediaItemDimensions.MEDIA_HEIGHT)
                .graphicsLayer { translationY = mediaOffsetPx }
                .clip(RoundedCornerShape(MediaItemDimensions.MEDIA_CORNER_RADIUS))) {
            MediaDisplaySection(
                cameraState = cameraState,
                currentCaption = currentCaption,
                onImageCaptureReady = cameraActions.onImageCaptureReady,
                onSnapshotHandlerReady = cameraActions.onSnapshotHandlerReady,
                onRequestPermission = cameraActions.onRequestPermission,
                onCaptionChange = cameraActions.onCaptionChange,
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .graphicsLayer { translationY = actionOffsetPx }) {
            CameraAction(
                modifier = Modifier.navigationBarsPadding(),
                capturedImagePath = cameraState.capturedImagePath,
                isCapturing = cameraState.isCapturing,
                onCapturePhoto = cameraActions.onCapturePhoto,
                onSwitchCamera = cameraActions.onSwitchCamera,
                onCancelCapture = cameraActions.onCancelCapture,
                onUploadPost = cameraActions.onUploadPost,
                isUploading = isUploading,
            )
        }
    }
}

@Composable
private fun MediaDisplaySection(
    cameraState: CameraState,
    currentCaption: String?,
    onImageCaptureReady: (ImageCapture) -> Unit,
    onSnapshotHandlerReady: (() -> Bitmap?) -> Unit,
    onRequestPermission: () -> Unit,
    onCaptionChange: (String) -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        if (cameraState.hasCameraPermission) {
            CameraPreview(
                modifier = Modifier.fillMaxSize(),
                onImageCaptureReady = onImageCaptureReady,
                onSnapshotHandlerReady = onSnapshotHandlerReady,
                shouldBindCamera = cameraState.shouldBindCamera,
                lensFacing = cameraState.lensFacing,
                placeholderBitmap = cameraState.lastPreviewSnapshot
            )
        } else {
            CameraPermissionDenied(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface),
                onRequestPermission = onRequestPermission
            )
        }

        cameraState.capturedImagePath?.let { path ->
            CapturedImageOverlay(
                imagePath = path,
                caption = currentCaption ?: "",
                onCaptionChange = onCaptionChange,
                isFrontCamera = cameraState.lensFacing == CameraSelector.LENS_FACING_FRONT
            )
        }
    }
}

@Composable
private fun CapturedImageOverlay(
    imagePath: String,
    caption: String,
    onCaptionChange: (String) -> Unit,
    isFrontCamera: Boolean = false
) {
    val imageUri = "file://$imagePath".toUri()

    Box(modifier = Modifier.fillMaxSize()) {
        AsyncImage(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { scaleX = if (isFrontCamera) -1f else 1f },
            imageUrl = imageUri.toString(),
            contentDescription = "Captured image",
            contentScale = ContentScale.Crop,
            resizeSize = ImageSize.Original,
            loadingConfig = LoadingStateConfig.None,
            crossfadeDuration = 0
        )

        CaptionInput(
            caption = caption,
            onCaptionChange = onCaptionChange,
            modifier = Modifier
                .zIndex(100f)
                .align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun CaptionInput(
    modifier: Modifier = Modifier,
    caption: String,
    onCaptionChange: (String) -> Unit,
) {
    Box(
        modifier = modifier
            .padding(bottom = 12.dp)
            .padding(horizontal = 12.dp)
    ) {
        TextField(
            value = caption, onValueChange = onCaptionChange, placeholder = {
                BaseText(
                    text = stringResource(R.string.add_caption_placeholder),
                    textAlign = TextAlign.Center,
                    typography = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
            }, colors = TextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedContainerColor = Color.Black.copy(alpha = 0.4f),
                unfocusedContainerColor = Color.Black.copy(alpha = 0.4f),
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                cursorColor = MaterialTheme.colorScheme.onSurface
            ), maxLines = 2, shape = CircleShape
        )
    }
}