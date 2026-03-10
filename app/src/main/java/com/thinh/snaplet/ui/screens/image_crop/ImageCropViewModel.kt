package com.thinh.snaplet.ui.screens.image_crop

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.thinh.snaplet.navigation.ImageCrop
import com.thinh.snaplet.utils.FileUtils.cropImageRegion
import com.thinh.snaplet.utils.FileUtils.saveBitmapToCache
import com.thinh.snaplet.utils.Logger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class ImageCropViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val route = savedStateHandle.toRoute<ImageCrop>()

    private val _uiState = MutableStateFlow(ImageCropUiState(imageUri = route.sourceUri))
    val uiState: StateFlow<ImageCropUiState> = _uiState.asStateFlow()

    fun cropImage(
        context: Context,
        uri: Uri,
        displayImageW: Int,
        displayImageH: Int,
        frameLeft: Float,
        frameTop: Float,
        frameW: Float,
        frameH: Float,
        rotationDeg: Int = 0,
        isFlippedH: Boolean = false,
        isFlippedV: Boolean = false,
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isCropping = true) }

            val croppedUri: Uri? = withContext(Dispatchers.IO) {
                val bitmap = cropImageRegion(
                    context, uri, displayImageW, displayImageH,
                    frameLeft, frameTop, frameW, frameH,
                    rotationDeg, isFlippedH, isFlippedV
                ) ?: return@withContext null
                saveBitmapToCache(context, bitmap)
            }

            _uiState.update { it.copy(isCropping = false, croppedUri = croppedUri) }
        }
    }
}
