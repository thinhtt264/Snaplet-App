package com.thinh.snaplet.ui.screens.my_profile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thinh.snaplet.R
import com.thinh.snaplet.data.model.user.AvatarUrls
import com.thinh.snaplet.data.repository.UserRepository
import com.thinh.snaplet.data.repository.auth.AuthRepository
import com.thinh.snaplet.domain.model.UploadAvatarResult
import com.thinh.snaplet.domain.user.UploadAvatarUseCase
import com.thinh.snaplet.platform.photo_picker.PhotoPickerManager
import com.thinh.snaplet.ui.common.UiText
import com.thinh.snaplet.ui.overlay.OverlayEventBus
import com.thinh.snaplet.ui.overlay.SheetOption
import com.thinh.snaplet.ui.theme.Red
import com.thinh.snaplet.utils.Logger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MyProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
    private val photoPickerManager: PhotoPickerManager,
    private val uploadAvatarUseCase: UploadAvatarUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        MyProfileUiState(
            widgetChainEnabled = true,
            showPhotoPicker = false,
            isAvatarChanging = false,
        )
    )

    val uiState: StateFlow<MyProfileUiState> = combine(
        _uiState,
        userRepository.observeMyUserProfile(),
    ) { state, profile ->
        state.copy(
            displayName = profile?.displayName.orEmpty(),
            firstName = profile?.firstName.orEmpty(),
            avatarUrls = profile?.avatarUrls ?: AvatarUrls(),
            userName = profile?.userName.orEmpty(),
            email = profile?.email.orEmpty(),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = _uiState.value
    )

    fun onWidgetChainToggle() {
        _uiState.update { it.copy(widgetChainEnabled = !it.widgetChainEnabled) }
    }

    fun onPhotoPickerLaunched() {
        _uiState.update { it.copy(showPhotoPicker = true) }
    }

    fun onPhotoPicked(uri: Uri) {
        _uiState.update { it.copy(isAvatarChanging = true) }
        viewModelScope.launch {
            try {
                val processedUri = photoPickerManager.processPickedImage(uri)
                val imagePath = processedUri?.path
                if (!imagePath.isNullOrBlank()) {
                    when (val result = uploadAvatarUseCase(imagePath)) {
                        is UploadAvatarResult.Success -> {
                            Logger.d("✅ Avatar uploaded successfully")
                        }

                        is UploadAvatarResult.Failed -> {
                            Logger.e("❌ Avatar upload failed: ${result.message}")
                        }
                    }
                } else {
                    Logger.e("❌ Avatar upload failed: processedUri.path is null or blank")
                }
            } finally {
                _uiState.update { it.copy(isAvatarChanging = false) }
            }
        }
    }

    fun onPhotoPickerDismissed() {
        _uiState.update { it.copy(showPhotoPicker = false) }
    }

    private fun handlePickFromGallery() {
        _uiState.update { it.copy(showPhotoPicker = true) }
    }

    private fun handleDeleteAvatar() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isAvatarChanging = true) }
                userRepository.deleteAvatar()
            } finally {
                _uiState.update { it.copy(isAvatarChanging = true) }
            }
        }
    }

    private fun handleLogout() {
        viewModelScope.launch {
            authRepository.logout()
        }
    }

    fun onLogout() {
        val options = listOf(
            SheetOption(
                id = "logout",
                label = UiText.StringResource(R.string.logout),
                color = Red,
                onClick = ::handleLogout
            ), SheetOption(
                id = "cancel", label = UiText.StringResource(R.string.cancel), onClick = { })
        )

        OverlayEventBus.showOptionsSheet(
            options = options,
            title = UiText.StringResource(R.string.profile_logout_confirm_title),
        )
    }

    fun onEditPhoto() {
        val options = listOf(
            SheetOption(
                id = "pick_from_gallery",
                label = UiText.StringResource(R.string.pick_photo_from_gallery),
                onClick = ::handlePickFromGallery,
            ), SheetOption(
                id = "delete_avatar",
                label = UiText.StringResource(R.string.delete_avatar),
                color = Red,
                onClick = ::handleDeleteAvatar,
            ), SheetOption(
                id = "cancel", label = UiText.StringResource(R.string.cancel), onClick = { })
        )

        OverlayEventBus.showOptionsSheet(
            options = options,
            title = UiText.StringResource(R.string.profile_change_avatar_title),
        )
    }
}
