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
private val ONLINE_PRESENCE_DOT_SIZE = 11.dp
private val ONLINE_PRESENCE_RING_WIDTH = 1.5.dp

// ─── LEVEL 1 ──────────────────────────────────────────────────
@Composable
private fun AvatarCore(
    avatarUrl: String?,
    firstName: String,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
) {
    val showInitial = avatarUrl.isNullOrBlank()

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (showInitial) {
            AvatarInitial(firstName = firstName)
        } else {
            AvatarImage(avatarUrl = avatarUrl, size = size)
        }
    }
}

// ─── LEVEL 2 — optional online presence dot (isOnline = null hides dot) ───
@Composable
fun Avatar(
    avatarUrl: String?,
    firstName: String,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    isOnline: Boolean? = null,
    presenceRingColor: Color = MaterialTheme.colorScheme.background,
) {
    if (isOnline == null) {
        AvatarCore(
            avatarUrl = avatarUrl,
            firstName = firstName,
            modifier = modifier,
            size = size,
        )
        return
    }

    Box(modifier = modifier.size(size)) {
        AvatarCore(
            avatarUrl = avatarUrl,
            firstName = firstName,
            modifier = Modifier.fillMaxSize(),
            size = size,
        )
        AvatarOnlinePresenceDot(
            isOnline = isOnline,
            ringColor = presenceRingColor,
            modifier = Modifier.align(Alignment.BottomEnd),
        )
    }
}

// ─── LEVEL 3 — connected-user ring ────────────────────────────
@Composable
fun Avatar(
    avatarUrl: String?,
    firstName: String,
    isConnectedUser: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    borderWidth: Dp = DEFAULT_AVATAR_BORDER_WIDTH,
    contentPadding: Dp = borderWidth * 2,
) {
    val borderColor = if (isConnectedUser) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceBright
    }

    Avatar(
        avatarUrl = avatarUrl,
        firstName = firstName,
        modifier = modifier
            .border(width = borderWidth, color = borderColor, shape = CircleShape)
            .padding(contentPadding),
        size = size,
    )
}

// ─── LEVEL 4 — connected-user ring + upload overlay ─────────────
@Composable
fun Avatar(
    avatarUrl: String?,
    firstName: String,
    isConnectedUser: Boolean,
    isUploading: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    borderWidth: Dp = DEFAULT_AVATAR_BORDER_WIDTH,
    contentPadding: Dp = borderWidth * 2,
) {
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Avatar(
            avatarUrl = avatarUrl,
            firstName = firstName,
            isConnectedUser = isConnectedUser,
            size = size,
            borderWidth = borderWidth,
            contentPadding = contentPadding
        )

        if (isUploading) {
            val innerSize = size - (borderWidth * 2) * 2
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
            .background(MaterialTheme.colorScheme.surface),
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
private fun AvatarOnlinePresenceDot(
    isOnline: Boolean,
    ringColor: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = Modifier
            .size(ONLINE_PRESENCE_DOT_SIZE)
            .clip(CircleShape)
            .background(
                if (isOnline) Color.Green else MaterialTheme.colorScheme.surfaceVariant,
            )
            .border(ONLINE_PRESENCE_RING_WIDTH, ringColor, CircleShape)
            .then(modifier),
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