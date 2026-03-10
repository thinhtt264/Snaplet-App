package com.thinh.snaplet.domain.post

import com.thinh.snaplet.data.model.user.UserProfile
import com.thinh.snaplet.data.repository.UserRepository
import javax.inject.Inject

class ValidateUploadPostUseCase @Inject constructor(
    private val userRepository: UserRepository
) {

    data class ValidatedInput(
        val imagePath: String,
        val caption: String?,
        val userProfile: UserProfile
    )

    sealed class ValidateUploadResult {
        data class Success(val input: ValidatedInput) : ValidateUploadResult()
        data object NoImage : ValidateUploadResult()
        data object AlreadyUploading : ValidateUploadResult()
        data object UserProfileNotFound : ValidateUploadResult()
    }

    suspend operator fun invoke(
        capturedImagePath: String?,
        caption: String?,
        isUploading: Boolean
    ): ValidateUploadResult {
        if (capturedImagePath.isNullOrBlank()) return ValidateUploadResult.NoImage
        if (isUploading) return ValidateUploadResult.AlreadyUploading

        val userProfile = userRepository.getCurrentUserProfile()
            ?: return ValidateUploadResult.UserProfileNotFound

        return ValidateUploadResult.Success(ValidatedInput(capturedImagePath, caption, userProfile))
    }
}
