package com.thinh.snaplet.domain.post

import com.thinh.snaplet.data.model.media.ImageTransform
import com.thinh.snaplet.data.repository.MediaRepository
import com.thinh.snaplet.domain.model.UploadPostResult
import com.thinh.snaplet.utils.network.ApiResult
import com.thinh.snaplet.utils.network.onFailure
import javax.inject.Inject

/**
 * Executes the full upload flow: request upload URL -> upload file -> confirm -> create post.
 * All business and network logic lives here; ViewModel only orchestrates and updates UI state.
 */
class UploadPostUseCase @Inject constructor(
    private val mediaRepository: MediaRepository
) {

    suspend operator fun invoke(
        imagePath: String,
        transform: ImageTransform,
        caption: String?,
        createAudience: PostCreateAudience = PostCreateAudience.FriendOnly,
    ): UploadPostResult {
        fun fail(step: String, result: ApiResult<*>): UploadPostResult.Failed {
            val error = (result as? ApiResult.Failure)?.error
            val message = error?.message ?: "Unknown error"
            return UploadPostResult.Failed(
                message = "$step: $message",
                apiError = error,
            )
        }

        return runCatching {
            val uploadRequestResult = mediaRepository.requestUpload(
                items = listOf(imagePath),
                transforms = listOf(transform)
            )
            val uploadRequestData = uploadRequestResult.fold(
                onSuccess = { it },
                onFailure = { return fail("Upload request failed", uploadRequestResult) }
            )

            if (uploadRequestData.data.isEmpty()) {
                return UploadPostResult.Failed("No upload URLs received")
            }

            val uploadItem = uploadRequestData.data.first()

            val uploadMediaResult = mediaRepository.uploadMedia(uploadItem.uploadUrl, imagePath)
            uploadMediaResult.onFailure { error ->
                return UploadPostResult.Failed(
                    message = "Upload failed: ${error.message}",
                    apiError = error,
                )
            }

            val confirmUploadResult = mediaRepository.confirmUpload(listOf(uploadItem.mediaId))
            confirmUploadResult.fold(
                onSuccess = { confirmData ->
                    val allowedViewerUserIds = when (createAudience) {
                        is PostCreateAudience.FriendOnly -> null
                        is PostCreateAudience.SelectedUsers -> createAudience.userIds
                    }
                    val createPostResult = mediaRepository.createPost(
                        mediaIds = confirmData.media.map { it.id },
                        caption = caption,
                        visibility = createAudience.apiVisibility,
                        allowedViewerUserIds = allowedViewerUserIds,
                    )
                    createPostResult.fold(
                        onSuccess = { createdPost -> UploadPostResult.Success(createdPost) },
                        onFailure = { fail("Upload failed", createPostResult) }
                    )
                },
                onFailure = { fail("Upload confirmation failed", confirmUploadResult) }
            )
        }.getOrElse { e ->
            UploadPostResult.Failed(e.message ?: "Unknown error")
        }
    }
}
