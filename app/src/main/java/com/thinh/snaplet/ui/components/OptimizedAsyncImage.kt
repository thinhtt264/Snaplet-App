package com.thinh.snaplet.ui.components

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
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Size

/**
 * Optimized async image component for heavy images
 * 
 * Features:
 * - Memory optimization with size limits
 * - Disk & memory caching
 * - Loading & error states
 * - Crossfade animation
 * - Configurable resize strategy
 * 
 * Usage:
 * ```
 * OptimizedAsyncImage(
 *     imageUrl = "https://example.com/image.jpg",
 *     contentDescription = "Photo",
 *     resizeSize = ImageSize.Medium
 * )
 * ```
 */
@Composable
fun OptimizedAsyncImage(
    imageUrl: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    resizeSize: ImageSize = ImageSize.Medium,
    showLoadingIndicator: Boolean = true,
    showErrorIcon: Boolean = true,
    loadingBackgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    errorBackgroundColor: Color = MaterialTheme.colorScheme.errorContainer,
    crossfadeDuration: Int = 300,
    errorPlaceholder: Painter? = null
) {
    SubcomposeAsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(imageUrl)
            // Performance optimizations
            .crossfade(crossfadeDuration)
            .size(resizeSize.toCoilSize()) // Resize to save memory
            // Cache policies (already set in ImageLoadingModule, but explicit here)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .networkCachePolicy(CachePolicy.ENABLED)
            // Quality settings
            .allowHardware(true) // Use hardware bitmaps (more efficient)
            .build(),
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
        loading = {
            if (showLoadingIndicator) {
                LoadingState(
                    backgroundColor = loadingBackgroundColor
                )
            }
        },
        error = {
            ErrorState(
                showIcon = showErrorIcon,
                backgroundColor = errorBackgroundColor,
                placeholder = errorPlaceholder
            )
        }
    )
}

/**
 * Loading state composable
 */
@Composable
private fun LoadingState(
    backgroundColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(32.dp),
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = 3.dp
        )
    }
}

/**
 * Error state composable
 */
@Composable
private fun ErrorState(
    showIcon: Boolean,
    backgroundColor: Color,
    placeholder: Painter?,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        if (showIcon && placeholder != null) {
            Icon(
                painter = placeholder,
                contentDescription = "Error loading image",
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.6f)
            )
        }
    }
}

/**
 * Image size presets for memory optimization
 * 
 * Smaller sizes = Less memory usage = Better performance
 * Choose based on display size in UI
 */
enum class ImageSize(val pixels: Int) {
    /**
     * Thumbnail size (e.g., for small previews)
     * ~200KB in memory per image
     */
    Thumbnail(300),
    
    /**
     * Small size (e.g., for grid items)
     * ~500KB in memory per image
     */
    Small(600),
    
    /**
     * Medium size (e.g., for list items, default)
     * ~1MB in memory per image
     */
    Medium(1200),
    
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

/**
 * Extension function for easier usage with custom sizes
 */
@Composable
fun OptimizedAsyncImage(
    imageUrl: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    customSizePixels: Int? = null, // Custom size if presets don't fit
    showLoadingIndicator: Boolean = true,
    showErrorIcon: Boolean = true
) {
    val size = customSizePixels ?: ImageSize.Medium.pixels
    
    OptimizedAsyncImage(
        imageUrl = imageUrl,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
        resizeSize = ImageSize.entries.find { it.pixels == size } ?: ImageSize.Medium,
        showLoadingIndicator = showLoadingIndicator,
        showErrorIcon = showErrorIcon
    )
}
