package com.thinh.snaplet.domain.model

sealed class UploadAvatarResult {
    data object Success : UploadAvatarResult()
    data class Failed(val message: String) : UploadAvatarResult()
}

