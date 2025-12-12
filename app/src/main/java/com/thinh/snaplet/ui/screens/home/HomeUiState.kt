package com.thinh.snaplet.ui.screens.home

import android.graphics.Bitmap
import com.thinh.snaplet.data.model.MediaItem

data class HomeUiState(
    val cameraState: CameraState,

    // Media feed states
    val mediaItems: List<MediaItem> = emptyList(),
    val isLoadingMedia: Boolean = false,

    // Error handling
    val error: String? = null
)

data class CameraState(
    val isCameraActive: Boolean = false,
    val isCapturing: Boolean = false,
    val showCameraPreview: Boolean = false,
    val lastPreviewSnapshot: Bitmap? = null,
    val hasCameraPermission: Boolean = false,
)

