package com.thinh.snaplet.ui.screens.home.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.Cached
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.NearMe
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.thinh.snaplet.ui.components.AppIconButton
import com.thinh.snaplet.ui.components.IconSpec
import com.thinh.snaplet.ui.theme.MotionTokens
import pressScaleClickable
import thenIf

private val CAPTURE_BUTTON_SIZE = 96.dp
private val ICON_SIZE = 46.dp
private val CAPTURE_BUTTON_BORDER_WIDTH = 4.dp
private const val CAPTURE_BUTTON_MIN_SCALE = 0.85f
private const val CAPTURE_BUTTON_MAX_SCALE = 1f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraAction(
    modifier: Modifier = Modifier,
    capturedImagePath: String?,
    isCapturing: Boolean = false,
    onCapturePhoto: () -> Unit,
    onCancelCapture: () -> Unit,
    onSwitchCamera: () -> Unit,
    onUploadPost: () -> Unit,
    isUploading: Boolean = false
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 36.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        val hasCaptureImage = capturedImagePath != null

        AnimatedContent(
            targetState = hasCaptureImage,
            transitionSpec = {
                fadeIn(animationSpec = tween(MotionTokens.Emphasized)) togetherWith fadeOut(
                    animationSpec = tween(MotionTokens.Emphasized)
                )
            },
            label = "camera_action_left_icon"
        ) { hasImage ->
            Icon(
                imageVector = if (hasImage) Icons.Outlined.Close else Icons.Outlined.AddPhotoAlternate,
                contentDescription = if (hasImage) "Cancel capture" else "Upload from gallery",
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .size(ICON_SIZE)
                    .thenIf(hasCaptureImage) { pressScaleClickable(onClick = onCancelCapture) }
            )
        }

        AnimatedContent(
            targetState = hasCaptureImage,
            transitionSpec = {
                fadeIn(animationSpec = tween(MotionTokens.Emphasized)) togetherWith fadeOut(
                    animationSpec = tween(MotionTokens.Emphasized)
                )
            },
            label = "camera_action_center",
        ) { hasImage ->
            if (hasImage) {
                AppIconButton(
                    modifier = Modifier.size(CAPTURE_BUTTON_SIZE),
                    containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    contentColor = MaterialTheme.colorScheme.onBackground,
                    iconSize = ICON_SIZE,
                    onClick = onUploadPost,
                    loading = isUploading,
                    icon = IconSpec.Vector(
                        Icons.Outlined.NearMe,
                        tint = MaterialTheme.colorScheme.onBackground,
                    )
                )
            } else {
                CaptureButton(
                    onCapturePhoto = onCapturePhoto,
                    isCapturing = isCapturing
                )
            }
        }

        Icon(
            Icons.Outlined.Cached,
            contentDescription = "Switch camera lens",
            tint = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .size(ICON_SIZE)
                .pressScaleClickable(onClick = onSwitchCamera),
        )
    }
}

@Composable
fun CaptureButton(
    modifier: Modifier = Modifier, onCapturePhoto: () -> Unit, isCapturing: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val effectivePressed = isPressed || isCapturing

    val backgroundColor by animateColorAsState(
        targetValue = if (effectivePressed) {
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        } else {
            Color.White
        },
        animationSpec = tween(durationMillis = MotionTokens.Emphasized),
        label = "capture_button_color"
    )

    val buttonScale by animateFloatAsState(
        targetValue = if (effectivePressed) {
            CAPTURE_BUTTON_MIN_SCALE
        } else {
            CAPTURE_BUTTON_MAX_SCALE
        }, animationSpec = spring(), label = "capture_button_scale"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "infinite transition")
    val borderPulseWidth by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.8f, animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = MotionTokens.Emphasized, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "border pulse width animation"
    )

    val density = LocalDensity.current
    val strokeWidthPx = if (effectivePressed) {
        with(density) { CAPTURE_BUTTON_BORDER_WIDTH.toPx() } * borderPulseWidth
    } else {
        with(density) { CAPTURE_BUTTON_BORDER_WIDTH.toPx() }
    }
    val borderOpacity = if (effectivePressed) {
        (borderPulseWidth - 1f) * 0.7f + 0.4f
    } else {
        1f
    }
    val borderColor = MaterialTheme.colorScheme.primary.copy(alpha = borderOpacity)

    Box(
        modifier = modifier
            .size(CAPTURE_BUTTON_SIZE),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val radius = (size.minDimension - strokeWidthPx) / 2f
            drawCircle(
                color = borderColor,
                center = center,
                radius = radius,
                style = Stroke(width = strokeWidthPx)
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .scale(buttonScale)
                .padding(8.dp)
                .background(color = backgroundColor, shape = CircleShape)
                .pressScaleClickable(
                    onClick = onCapturePhoto,
                    interactionSource = interactionSource,
                    enabled = !isCapturing
                )
        )
    }
}