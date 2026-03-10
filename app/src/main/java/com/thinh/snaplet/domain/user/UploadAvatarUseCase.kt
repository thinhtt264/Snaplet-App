package com.thinh.snaplet.domain.user

import com.thinh.snaplet.data.repository.UserRepository
import com.thinh.snaplet.domain.model.UploadAvatarResult
import com.thinh.snaplet.utils.network.onFailure
import javax.inject.Inject

/**
 * Executes the full avatar upload flow:
 * request avatar upload URL -> upload file -> confirm avatar.
 */
class UploadAvatarUseCase @Inject constructor(
    private val userRepository: UserRepository
) {

    suspend operator fun invoke(
        imagePath: String,
    ): UploadAvatarResult {
        return runCatching {
            val uploadRequest = userRepository.requestAvatarUpload(
                filePath = imagePath,
            ).fold(
                onSuccess = { it },
                onFailure = { return UploadAvatarResult.Failed("Avatar upload request failed: ${it.message}") }
            )

            userRepository.uploadAvatar(
                uploadUrl = uploadRequest.uploadUrl,
                filePath = imagePath,
            ).onFailure { error ->
                return UploadAvatarResult.Failed("Avatar upload failed: ${error.message}")
            }

            userRepository.confirmAvatarUpload(
                key = uploadRequest.key,
            ).fold(
                onSuccess = { UploadAvatarResult.Success },
                onFailure = { UploadAvatarResult.Failed("Avatar upload confirmation failed: ${it.message}") }
            )
        }.getOrElse { e ->
            UploadAvatarResult.Failed(e.message ?: "Unknown error")
        }
    }
}

