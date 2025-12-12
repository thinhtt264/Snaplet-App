package com.thinh.snaplet.ui.screens.home.components

import android.graphics.Bitmap
import androidx.camera.core.ImageCapture
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.thinh.snaplet.R
import com.thinh.snaplet.ui.components.AnimatedButton
import com.thinh.snaplet.ui.components.AppText
import com.thinh.snaplet.ui.components.CameraPreview
import com.thinh.snaplet.ui.components.PrimaryButton
import com.thinh.snaplet.ui.screens.home.CameraState

private val CAPTURE_BUTTON_TOP_PADDING = 56.dp
private val CAPTURE_BUTTON_SIZE = 80.dp
private val CAPTURE_BUTTON_INNER_SIZE = 60.dp
private val CAPTURE_BUTTON_BORDER_WIDTH = 6.dp
private const val CAPTURE_BUTTON_SCALE = 0.85f
private const val CAPTURE_ANIMATION_DURATION = 150

@Composable
fun CameraPage(
    cameraState: CameraState,
    onImageCaptureReady: (ImageCapture) -> Unit,
    onSnapshotHandlerReady: (() -> Bitmap?) -> Unit,
    shouldBindCamera: Boolean,
    onRequestPermission: () -> Unit,
    onCapturePhoto: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CameraPreviewSection(
            cameraState = cameraState,
            onImageCaptureReady = onImageCaptureReady,
            onSnapshotHandlerReady = onSnapshotHandlerReady,
            shouldBindCamera = shouldBindCamera,
            onRequestPermission = onRequestPermission
        )

        CaptureButton(
            onClick = onCapturePhoto,
            modifier = Modifier.padding(top = CAPTURE_BUTTON_TOP_PADDING)
        )
    }
}

@Composable
private fun CameraPreviewSection(
    cameraState: CameraState,
    onImageCaptureReady: (ImageCapture) -> Unit,
    onSnapshotHandlerReady: (() -> Bitmap?) -> Unit,
    shouldBindCamera: Boolean,
    onRequestPermission: () -> Unit
) {
    val cameraModifier = Modifier
        .fillMaxWidth()
        .height(MediaItemDimensions.MEDIA_HEIGHT)
        .padding(top = MediaItemDimensions.MEDIA_TOP_PADDING)
        .clip(RoundedCornerShape(MediaItemDimensions.MEDIA_CORNER_RADIUS))

    if (cameraState.hasCameraPermission) {
        CameraPreview(
            modifier = cameraModifier,
            onImageCaptureReady = onImageCaptureReady,
            onSnapshotHandlerReady = onSnapshotHandlerReady,
            shouldBindCamera = shouldBindCamera,
            placeholderBitmap = cameraState.lastPreviewSnapshot
        )
    } else {
        CameraPermissionDenied(
            modifier = cameraModifier.background(MaterialTheme.colorScheme.surface),
            onRequestPermission = onRequestPermission
        )
    }
}

@Composable
private fun CameraPermissionDenied(
    modifier: Modifier = Modifier,
    onRequestPermission: () -> Unit
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        ) {
            AppText(
                text = stringResource(R.string.camera_unavailable),
                typography = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            AppText(
                modifier = Modifier.padding(horizontal = 32.dp),
                text = stringResource(R.string.approve_camera),
                typography = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.secondary,
                textAlign = TextAlign.Center,
            )
            PrimaryButton(
                onClick = onRequestPermission,
                title = stringResource(R.string.approve),
                titleColor = Color.Black,
                contentPadding = PaddingValues(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
            )
        }
    }
}

@Composable
private fun CaptureButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val backgroundColor by animateColorAsState(
        targetValue = if (isPressed) {
            MaterialTheme.colorScheme.secondary
        } else {
            Color.White
        },
        animationSpec = tween(durationMillis = CAPTURE_ANIMATION_DURATION),
        label = "capture_button_color"
    )

    Box(
        modifier = modifier
            .size(CAPTURE_BUTTON_SIZE)
            .border(
                width = CAPTURE_BUTTON_BORDER_WIDTH,
                color = MaterialTheme.colorScheme.primary,
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        AnimatedButton(
            modifier = Modifier.size(CAPTURE_BUTTON_INNER_SIZE),
            onClick = onClick,
            scaleOnPress = CAPTURE_BUTTON_SCALE,
            interactionSource = interactionSource
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        color = backgroundColor,
                        shape = CircleShape
                    )
            )
        }
    }
}
