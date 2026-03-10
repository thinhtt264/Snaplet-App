package com.thinh.snaplet.ui.screens.image_crop

import android.net.Uri

data class ImageCropUiState(
    val imageUri: String? = null,
    val croppedUri: Uri? = null,
    val isCropping: Boolean = false
)
