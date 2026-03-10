package com.thinh.snaplet.ui.screens.my_profile

import com.thinh.snaplet.data.model.user.AvatarUrls

data class MyProfileUiState(
    val displayName: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val avatarUrls: AvatarUrls = AvatarUrls(),
    val userName: String = "",
    val email: String = "",
    val widgetChainEnabled: Boolean = true,
    val showPhotoPicker: Boolean = false,
    val isAvatarChanging: Boolean = false,
    val isEditNameSheetVisible: Boolean = false,
    val editFirstName: String = "",
    val editLastName: String = "",
    val isUpdatingDisplayName: Boolean = false,
)
