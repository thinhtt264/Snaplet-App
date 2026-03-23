package com.thinh.snaplet.ui.screens.home.components

import android.graphics.Bitmap
import android.os.SystemClock
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.net.toUri
import com.thinh.snaplet.R
import com.thinh.snaplet.ui.components.AppIconButton
import com.thinh.snaplet.ui.components.BaseText
import com.thinh.snaplet.ui.components.CameraPreview
import com.thinh.snaplet.ui.components.CappedCountBadge
import com.thinh.snaplet.ui.components.IconSpec
import com.thinh.snaplet.ui.components.image.AsyncImage
import com.thinh.snaplet.ui.components.image.ImageSize
import com.thinh.snaplet.ui.components.image.LoadingStateConfig
import com.thinh.snaplet.ui.screens.home.CameraActions
import com.thinh.snaplet.ui.screens.home.CameraState
import com.thinh.snaplet.ui.theme.MotionTokens
import com.thinh.snaplet.ui.theme.Typography
import com.thinh.snaplet.utils.ValidationConstants
import kotlinx.coroutines.delay

private const val TOP_SPACE_RATIO = 0.11f
private const val SHAKE_DURATION = 500
private const val SHAKE_COOLDOWN = 600L

private val SHAKE_KEYFRAMES: List<Pair<Float, Int>> = listOf(
    -6f to 50,
    6f to 110,
    -5f to 170,
    5f to 230,
    -3f to 290,
    3f to 350,
    0f to 430,
)

@Composable
fun CameraPage(
    cameraState: CameraState,
    currentCaption: String?,
    isUploading: Boolean,
    cameraActions: CameraActions,
    onDownloadImage: () -> Unit,
    unreadPostsCount: Int,
    onHistoryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier.fillMaxSize()
    ) {
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
            with(density) { (screenHeight * (2f / 3f)).toPx() }
        }

        var imeFullHeightPx by remember { mutableFloatStateOf(imeHeightPx) }
        if (imeHeightPx > imeFullHeightPx) {
            imeFullHeightPx = imeHeightPx
        }

        val topActionsAlpha = when {
            cameraState.capturedImagePath == null -> 0f
            imeFullHeightPx == 0f -> 1f
            else -> 1f - (imeHeightPx / imeFullHeightPx).coerceIn(0f, 1f)
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(all = 16.dp)
                .graphicsLayer { alpha = topActionsAlpha },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(modifier = Modifier.weight(1f))

            BaseText(
                text = stringResource(id = R.string.send_to),
                color = Color.White,
                textAlign = TextAlign.Center,
                typography = Typography.titleMedium
            )

            var showCheckIcon by remember { mutableStateOf(false) }
            LaunchedEffect(showCheckIcon) {
                if (showCheckIcon) {
                    delay(2000)
                    showCheckIcon = false
                }
            }
            Box(
                modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd
            ) {
                AnimatedContent(
                    targetState = showCheckIcon, transitionSpec = {
                        fadeIn(animationSpec = tween(durationMillis = MotionTokens.Slow)).togetherWith(
                            fadeOut(animationSpec = tween(durationMillis = MotionTokens.Slow))
                        )
                    }, label = "download_icon"
                ) { isCheck ->
                    AppIconButton(
                        containerColor = Color.Transparent, onClick = {
                            if (!isCheck) {
                                onDownloadImage()
                                showCheckIcon = true
                            }
                        }, iconSize = 32.dp, icon = IconSpec.Vector(
                            if (isCheck) Icons.Outlined.CheckCircle else Icons.Outlined.FileDownload,
                            tint = Color.White
                        )
                    )
                }
            }
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

        if (cameraState.capturedImagePath == null) {
            HistoryButton(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = 6.dp, bottom = 28.dp)
                    .navigationBarsPadding(),
                unreadPostsCount = unreadPostsCount,
                onClick = onHistoryClick,
            )
        }
    }
}

@Composable
private fun HistoryButton(
    modifier: Modifier = Modifier,
    unreadPostsCount: Int,
    onClick: () -> Unit,
) {
    val hasUnread = unreadPostsCount > 0

    Box(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable(onClick = onClick)
        ) {
            AnimatedContent(
                targetState = hasUnread, label = "HistoryButtonBadge"
            ) { showBadge ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.animateContentSize()
                ) {
                    if (showBadge) {
                        CappedCountBadge(
                            count = unreadPostsCount,
                            modifier = Modifier.size(24.dp),
                            backgroundColor = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(2.dp),
                        )
                    }

                    BaseText(
                        text = stringResource(id = R.string.history_label),
                        color = MaterialTheme.colorScheme.onBackground,
                        typography = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(start = 8.dp)
                    )

                    Icon(
                        Icons.Outlined.KeyboardArrowDown,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
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
    val focusManager = LocalFocusManager.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(onTap = { focusManager.clearFocus() })
            }) {
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
    var shakeTrigger by remember { mutableIntStateOf(0) }
    var lastShakeTime by remember { mutableLongStateOf(0L) }
    val shakeAngle = remember { Animatable(0f) }

    LaunchedEffect(shakeTrigger) {
        if (shakeTrigger == 0) return@LaunchedEffect

        val now = SystemClock.elapsedRealtime()
        if (now - lastShakeTime < SHAKE_COOLDOWN) return@LaunchedEffect
        lastShakeTime = now

        try {
            shakeAngle.snapTo(0f)
            shakeAngle.animateTo(
                targetValue = 0f, animationSpec = keyframes {
                    durationMillis = SHAKE_DURATION
                    SHAKE_KEYFRAMES.forEach { (angle, time) ->
                        angle at time using LinearEasing
                    }
                })
        } finally {
            shakeAngle.snapTo(0f)
        }
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val captionTextStyle = MaterialTheme.typography.titleSmall.copy(
        fontWeight = FontWeight.Bold,
        color = Color.White,
        textAlign = TextAlign.Center,
        lineHeight = MaterialTheme.typography.titleSmall.fontSize,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    )

    val density = LocalDensity.current
    val lineCount = remember { mutableIntStateOf(1) }

    val fixedHeight by remember {
        derivedStateOf {
            with(density) {
                captionTextStyle.fontSize.toDp() * lineCount.intValue + 24.dp
            }
        }
    }

    Box(
        modifier = modifier
            .padding(bottom = 12.dp)
            .padding(horizontal = 12.dp)
            .graphicsLayer {
                rotationZ = shakeAngle.value
                transformOrigin = TransformOrigin(0.5f, 0.5f)
            },
    ) {
        BasicTextField(
            onTextLayout = { result ->
                lineCount.intValue = result.lineCount
            },
            interactionSource = interactionSource,
            value = caption.take(ValidationConstants.CAPTION_MAX_LENGTH),
            onValueChange = { newValue ->
                val filtered = newValue.replace("\n", "")
                if (filtered.length > ValidationConstants.CAPTION_MAX_LENGTH) {
                    shakeTrigger++
                } else {
                    onCaptionChange(filtered)
                }
            },
            modifier = Modifier
                .widthIn(
                    min = Dp.Unspecified, max = MediaItemDimensions.CAPTION_CONTAINER_MAX_WIDTH
                )
                .wrapContentWidth(),
            textStyle = captionTextStyle,
            maxLines = 2,
            cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier
                        .background(
                            color = Color.Black.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .height(fixedHeight)
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (caption.isEmpty()) {
                        BaseText(
                            text = stringResource(R.string.add_caption_placeholder),
                            typography = captionTextStyle,
                            color = if (isFocused) MaterialTheme.colorScheme.onSurface else Color.White
                        )
                    }
                    innerTextField()
                }
            })
    }
}