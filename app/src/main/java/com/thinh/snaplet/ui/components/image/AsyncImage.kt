package com.thinh.snaplet.ui.components.image

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Size
import com.thinh.snaplet.ui.theme.MotionTokens

// ===== AsyncImage - Level 1: Simplest =====
@Composable
fun AsyncImage(
    modifier: Modifier = Modifier,
    imageUrl: String,
    contentDescription: String?,
) = AsyncImage(
    modifier = modifier,
    imageUrl = imageUrl,
    contentDescription = contentDescription,
    loadingConfig = LoadingStateConfig.Indicator(),
    errorConfig = ErrorStateConfig()
)

// ===== AsyncImage - Level 2: Quick error placeholder =====

@Composable
fun AsyncImage(
    modifier: Modifier = Modifier,
    imageUrl: String,
    contentDescription: String?,
    errorPlaceholder: ErrorPlaceholderConfig,
) = AsyncImage(
    modifier = modifier,
    imageUrl = imageUrl,
    contentDescription = contentDescription,
    loadingConfig = LoadingStateConfig.Indicator(),
    errorConfig = ErrorStateConfig(placeholder = errorPlaceholder)
)

// ===== AsyncImage - Level 3: Full custom =====
@Composable
fun AsyncImage(
    modifier: Modifier = Modifier,
    imageUrl: String,
    contentDescription: String?,
    loadingConfig: LoadingStateConfig = LoadingStateConfig.Indicator(),
    errorConfig: ErrorStateConfig = ErrorStateConfig(),
    contentScale: ContentScale = ContentScale.Crop,
    resizeSize: ImageSize = ImageSize.Small,
    crossfadeDuration: Int = 300,
) {
    SubcomposeAsyncImage(
        modifier = modifier,
        model = ImageRequest.Builder(LocalContext.current).data(imageUrl)
            .crossfade(crossfadeDuration).size(resizeSize.toCoilSize())
            .memoryCachePolicy(CachePolicy.ENABLED).diskCachePolicy(CachePolicy.ENABLED)
            .networkCachePolicy(CachePolicy.ENABLED).allowHardware(true).build(),
        contentDescription = contentDescription,
        contentScale = contentScale
    ) {
        val uiState = when (painter.state) {
            is AsyncImagePainter.State.Success -> AsyncImageUiState.Success
            is AsyncImagePainter.State.Loading -> AsyncImageUiState.Loading
            else -> AsyncImageUiState.Error
        }

        AnimatedContent(
            targetState = uiState,
            transitionSpec = {
                fadeIn(
                    animationSpec = tween(durationMillis = MotionTokens.Emphasized)
                ) togetherWith fadeOut(
                    animationSpec = tween(durationMillis = MotionTokens.Emphasized)
                )
            },
            label = "AsyncImageStateAnimatedContent",
        ) { target ->
            when (target) {
                AsyncImageUiState.Success -> {
                    Image(
                        painter = painter,
                        contentDescription = contentDescription,
                        contentScale = contentScale,
                        modifier = Modifier.matchParentSize()
                    )
                }

                AsyncImageUiState.Loading -> {
                    AsyncImageLoadingState(
                        config = loadingConfig,
                        modifier = Modifier.matchParentSize()
                    )
                }

                AsyncImageUiState.Error -> {
                    AsyncImageErrorState(
                        config = errorConfig,
                        modifier = Modifier.matchParentSize()
                    )
                }
            }
        }
    }
}

private enum class AsyncImageUiState {
    Loading,
    Success,
    Error,
}

@Composable
private fun AsyncImageLoadingState(
    config: LoadingStateConfig, modifier: Modifier = Modifier
) {
    when (config) {
        is LoadingStateConfig.Indicator -> {
            val bgColor =
                if (config.backgroundColor == Color.Unspecified) MaterialTheme.colorScheme.surface else config.backgroundColor
            val indicatorColor =
                if (config.color == Color.Unspecified) MaterialTheme.colorScheme.primary else config.color

            Box(
                modifier = modifier
                    .fillMaxSize()
                    .background(bgColor),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(config.indicatorSize),
                    color = indicatorColor,
                    strokeWidth = config.strokeWidth
                )
            }
        }

        is LoadingStateConfig.Placeholder -> {
            val bgColor =
                if (config.backgroundColor == Color.Unspecified) MaterialTheme.colorScheme.surface else config.backgroundColor

            Box(
                modifier = modifier
                    .fillMaxSize()
                    .background(bgColor),
                contentAlignment = Alignment.Center
            ) {
                Image(painter = config.painter, contentDescription = null)
            }
        }

        LoadingStateConfig.None -> Unit
    }
}

@Composable
private fun AsyncImageErrorState(
    config: ErrorStateConfig, modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(config.backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        when (val placeholder = config.placeholder) {
            is ErrorPlaceholderConfig.WithPainter -> {
                Icon(
                    painter = placeholder.painter,
                    contentDescription = null,
                    modifier = Modifier.size(placeholder.size),
                    tint = Color.Unspecified
                )
            }

            is ErrorPlaceholderConfig.WithIcon -> {
                val tint =
                    if (placeholder.tint == Color.Unspecified) MaterialTheme.colorScheme.onErrorContainer else placeholder.tint

                Icon(
                    imageVector = placeholder.imageVector,
                    contentDescription = null,
                    modifier = Modifier.size(placeholder.size),
                    tint = tint
                )
            }

            is ErrorPlaceholderConfig.WithIconRes -> {
                val tint =
                    if (placeholder.tint == Color.Unspecified) MaterialTheme.colorScheme.onErrorContainer else placeholder.tint

                Icon(
                    painter = painterResource(placeholder.iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(placeholder.size),
                    tint = tint
                )
            }

            ErrorPlaceholderConfig.None -> Unit
        }
    }
}

enum class ImageSize(val pixels: Int) {
    /**
     * Thumbnail size (e.g., for small previews)
     * ~200KB in memory per image
     */
    Thumbnail(256),

    /**
     * Small size (e.g., for grid items)
     * ~500KB in memory per image
     */
    Small(512),

    /**
     * Medium size (e.g., for list items, default)
     * ~1MB in memory per image
     */
    Medium(1024),

    /**
     * Large size (e.g., for detail screens)
     * ~2MB in memory per image
     */
    Large(2048),

    /**
     * Original size (no resize)
     * Use sparingly - can be 5-10MB per image!
     */
    Original(Int.MAX_VALUE);

    /**
     * Convert to Coil's Size
     */
    fun toCoilSize(): Size {
        return if (this == Original) {
            Size.ORIGINAL
        } else {
            Size(pixels, pixels)
        }
    }
}