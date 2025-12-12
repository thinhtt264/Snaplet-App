package com.thinh.snaplet.ui.screens.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.thinh.snaplet.data.model.MediaItem
import com.thinh.snaplet.ui.components.ImageSize
import com.thinh.snaplet.ui.components.OptimizedAsyncImage

@Composable
fun MediaItemPage(
    media: MediaItem,
    onMediaClick: (MediaItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Full-screen image/video
        OptimizedAsyncImage(
            imageUrl = media.url,
            contentDescription = "Media ${media.id}",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            resizeSize = ImageSize.Large, // High quality for full-screen
            showLoadingIndicator = true,
            showErrorIcon = true
        )

        // Video indicator if needed
        if (media is MediaItem.Video) {
            VideoIndicatorOverlay(
                durationMs = media.durationMs,
                modifier = Modifier.align(Alignment.BottomEnd)
            )
        }

        // Future: Add TikTok-style UI overlays
        // - Like, Comment, Share buttons on the right
        // - User info, description at the bottom
        // - Progress indicator for videos
    }
}

/** Video indicator overlay (for future video support) */
@Composable
private fun VideoIndicatorOverlay(
    durationMs: Long,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .padding(8.dp)
            .background(
                color = Color.Black.copy(alpha = 0.7f),
                shape = RoundedCornerShape(4.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        // Future: Show duration text
        // AppText(text = formatDuration(durationMs))
    }
}