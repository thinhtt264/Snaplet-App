package com.thinh.snaplet.ui.screens.home

import com.thinh.snaplet.data.model.MediaItem

/**
 * UI State for Home Screen
 * Represents all possible states of the screen
 * Following MVI (Model-View-Intent) pattern
 */
data class HomeUiState(
    // Camera states
    val hasCameraPermission: Boolean = false,
    val isCameraReady: Boolean = false,
    val isCapturing: Boolean = false,
    
    // Media feed states
    val mediaItems: List<MediaItem> = emptyList(),
    val isLoadingMedia: Boolean = false,
    
    // Error handling
    val error: String? = null
)

