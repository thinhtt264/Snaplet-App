package com.thinh.snaplet.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thinh.snaplet.ui.common.CommonImages
import com.thinh.snaplet.ui.components.image.AsyncImage
import com.thinh.snaplet.ui.components.image.ErrorPlaceholderConfig
import com.thinh.snaplet.ui.components.image.ErrorStateConfig
import com.thinh.snaplet.ui.components.image.ImageSize
import com.thinh.snaplet.ui.components.image.LoadingStateConfig

private val DEFAULT_AVATAR_BORDER_WIDTH = 2.dp
private val AVATAR_BORDER_GAP = 4.dp

// ─── LEVEL 1 ──────────────────────────────────────────────────
@Composable
fun Avatar(
    avatarUrl: String?,
    firstName: String,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
) {
    val showInitial = avatarUrl.isNullOrBlank()

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape), contentAlignment = Alignment.Center
    ) {
        if (showInitial) {
            AvatarInitial(firstName = firstName)
        } else {
            AvatarImage(avatarUrl = avatarUrl, size = size)
        }
    }
}

// ─── LEVEL 2 ──────────────────────────────────────────────────
@Composable
fun Avatar(
    avatarUrl: String?,
    firstName: String,
    isConnectedUser: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    borderWidth: Dp = DEFAULT_AVATAR_BORDER_WIDTH,
) {
    val borderColor = if (isConnectedUser) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.secondary
    }

    Avatar(
        avatarUrl = avatarUrl,
        firstName = firstName,
        modifier = modifier
            .border(width = borderWidth, color = borderColor, shape = CircleShape)
            .padding(borderWidth + AVATAR_BORDER_GAP),
        size = size,
    )
}

// ─── LEVEL 3 ──────────────────────────────────────────────────
@Composable
fun Avatar(
    avatarUrl: String?,
    firstName: String,
    isConnectedUser: Boolean,
    isUploading: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    borderWidth: Dp = DEFAULT_AVATAR_BORDER_WIDTH,
) {
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Avatar(
            avatarUrl = avatarUrl,
            firstName = firstName,
            isConnectedUser = isConnectedUser,
            size = size,
            borderWidth = borderWidth,
        )

        if (isUploading) {
            val innerSize = size - (borderWidth + AVATAR_BORDER_GAP) * 2
            AvatarUploadingOverlay(size = innerSize)
        }
    }
}

@Composable
private fun AvatarInitial(firstName: String) {
    val initial = firstName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.secondary),
        contentAlignment = Alignment.Center
    ) {
        val fontSize = (minOf(maxWidth, maxHeight).value * 0.4f).sp
        BaseText(
            text = initial, typography = MaterialTheme.typography.titleLarge.copy(
                fontSize = fontSize, fontWeight = FontWeight.SemiBold
            ), color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun AvatarImage(avatarUrl: String, size: Dp) {
    AsyncImage(
        imageUrl = avatarUrl,
        contentDescription = null,
        modifier = Modifier.fillMaxSize(),
        resizeSize = ImageSize.Thumbnail,
        loadingConfig = LoadingStateConfig.Indicator(indicatorSize = size / 3),
        errorConfig = ErrorStateConfig(
            backgroundColor = Color.Transparent, placeholder = ErrorPlaceholderConfig.WithPainter(
                painter = painterResource(CommonImages.ProfilePlaceholder), size = size
            )
        )
    )
}

@Composable
private fun AvatarUploadingOverlay(size: Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .background(Color.Black.copy(alpha = 0.7f), shape = CircleShape),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(size / 3),
            strokeWidth = 3.dp
        )
    }
}