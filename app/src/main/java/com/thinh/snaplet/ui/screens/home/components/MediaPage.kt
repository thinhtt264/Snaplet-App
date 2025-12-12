package com.thinh.snaplet.ui.screens.home.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.thinh.snaplet.ui.screens.home.HomeUiState
import com.thinh.snaplet.utils.Logger

@Composable
fun MediaPage(
    page: Int,
    uiState: HomeUiState
) {
    val cameraModifier = Modifier
        .fillMaxWidth()
        .height(MediaItemDimensions.MEDIA_HEIGHT)
        .padding(top = MediaItemDimensions.MEDIA_TOP_PADDING)
        .clip(RoundedCornerShape(MediaItemDimensions.MEDIA_CORNER_RADIUS))

    val mediaIndex = page - 1
    if (mediaIndex < uiState.mediaItems.size) {
        MediaItemPage(
            media = uiState.mediaItems[mediaIndex],
            onMediaClick = { media ->
                Logger.d("📷 Clicked media: ${media.id}")
            },
            modifier = cameraModifier
        )
    }
}