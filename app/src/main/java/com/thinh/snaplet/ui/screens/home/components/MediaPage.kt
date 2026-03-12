package com.thinh.snaplet.ui.screens.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.thinh.snaplet.R
import com.thinh.snaplet.data.model.post.Post
import com.thinh.snaplet.ui.common.CommonImages
import com.thinh.snaplet.ui.components.Avatar
import com.thinh.snaplet.ui.components.BaseText
import com.thinh.snaplet.ui.components.image.AsyncImage
import com.thinh.snaplet.ui.components.image.ErrorPlaceholderConfig
import com.thinh.snaplet.ui.components.image.ErrorStateConfig
import com.thinh.snaplet.ui.components.image.ImageSize
import com.thinh.snaplet.ui.screens.home.UploadStatus
import com.thinh.snaplet.ui.theme.Error50
import com.thinh.snaplet.utils.formatTimeAgo

private const val TOP_SPACE_RATIO = 0.15f

@Composable
fun MediaPage(
    post: Post,
    uploadStatus: UploadStatus?,
    showBottomAction: Boolean = false,
    showMoreButtonLoading: Boolean = false,
    onGridClick: () -> Unit = {},
    onCaptureClick: () -> Unit = {},
    onMoreClick: () -> Unit = {},
    onRetryClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {},
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val screenHeight = maxHeight
        val topPadding = screenHeight * TOP_SPACE_RATIO
        val isUploadFailed = uploadStatus is UploadStatus.Failed

        Column(
            modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(topPadding))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(MediaItemDimensions.MEDIA_HEIGHT)
                    .clip(RoundedCornerShape(MediaItemDimensions.MEDIA_CORNER_RADIUS))
            ) {
                PostMediaContent(post = post)

                if (isUploadFailed) {
                    UploadFailedOverlay(onRetryClick = onRetryClick)
                }
            }

            Spacer(Modifier.height(12.dp))

            PostMetadata(
                post = post, uploadStatus = uploadStatus, onDeleteClick = onDeleteClick
            )
        }

        if (showBottomAction) {
            BottomAction(
                onGridClick = onGridClick,
                onCaptureClick = onCaptureClick,
                onMoreClick = onMoreClick,
                showMoreButtonLoading = showMoreButtonLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 32.dp, vertical = 24.dp)
            )
        }
    }
}

@Composable
private fun PostMediaContent(post: Post) {
    val media = post.media.firstOrNull() ?: return
    val transform = media.transform

    Box(modifier = Modifier.fillMaxSize()) {
        AsyncImage(
            imageUrl = media.images.md,
            contentDescription = "Post ${post.id}",
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    transform?.let {
                        rotationZ = it.rotation.toFloat()
                        scaleX = it.scaleX
                        scaleY = it.scaleY
                    }
                },
            resizeSize = ImageSize.Small,
            contentScale = ContentScale.Crop,
            errorConfig = ErrorStateConfig(
                backgroundColor = MaterialTheme.colorScheme.surface,
                placeholder = ErrorPlaceholderConfig.WithPainter(
                    painter = painterResource(CommonImages.PhotoPlaceholder), size = 84.dp
                )
            )
        )

        if (!post.caption.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .zIndex(99f)
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp)
                    .padding(horizontal = 12.dp)
                    .background(
                        color = Color.Black.copy(alpha = 0.4f), shape = RoundedCornerShape(16.dp)
                    )
                    .padding(vertical = 6.dp, horizontal = 12.dp)
                    .widthIn(max = MediaItemDimensions.CAPTION_CONTAINER_MAX_WIDTH)
            ) {
                BaseText(
                    text = post.caption,
                    typography = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun UploadFailedOverlay(onRetryClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(onClick = onRetryClick)
            .zIndex(100f), contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Refresh,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = Color.White
            )
            BaseText(
                text = stringResource(R.string.upload_failed_tap_to_retry),
                typography = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp)
                .size(28.dp)
                .background(color = Error50, shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Warning,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = Color.White
            )
        }
    }
}

@Composable
private fun PostMetadata(
    modifier: Modifier = Modifier,
    post: Post,
    uploadStatus: UploadStatus?,
    onDeleteClick: () -> Unit = {}
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (uploadStatus) {
            is UploadStatus.Uploading -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onBackground,
                    strokeWidth = 2.dp
                )
                BaseText(
                    text = stringResource(R.string.uploading),
                    typography = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            is UploadStatus.Failed -> {
                IconButton(onClick = onDeleteClick) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "delete",
                        modifier = Modifier.size(36.dp),
                        tint = Error50
                    )
                }
            }

            else -> {
                if (post.isOwnPost) {
                    BaseText(
                        text = stringResource(R.string.you),
                        typography = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Avatar(
                        avatarUrl = post.avatarUrls.forThumbnail(),
                        firstName = post.firstName,
                        size = 32.dp,
                    )

                    BaseText(
                        text = post.firstName,
                        typography = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                BaseText(
                    text = " ${formatTimeAgo(post.createdAt)}",
                    typography = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}