package com.thinh.snaplet.domain.post

import com.thinh.snaplet.data.model.media.ImageTransform
import com.thinh.snaplet.data.model.post.Post
import javax.inject.Inject

class ValidateRetryUploadUseCase @Inject constructor() {

    data class RetryInput(
        val tempPostId: String,
        val imagePath: String,
        val transform: ImageTransform,
        val caption: String?
    )

    sealed class ValidateRetryResult {
        data class Success(val input: RetryInput) : ValidateRetryResult()
        data object PostNotFound : ValidateRetryResult()
        data object MediaNotFound : ValidateRetryResult()
        data object ImagePathNotFound : ValidateRetryResult()
    }

    operator fun invoke(tempPost: Post?): ValidateRetryResult {
        val post = tempPost ?: return ValidateRetryResult.PostNotFound

        val media = post.media.firstOrNull()
            ?: return ValidateRetryResult.MediaNotFound

        val imagePath = media.images.original.removePrefix("file://").takeIf { it.isNotBlank() }
            ?: return ValidateRetryResult.ImagePathNotFound

        val transform = media.transform ?: ImageTransform(rotation = 0, scaleX = 1f, scaleY = 1f)

        return ValidateRetryResult.Success(
            RetryInput(
                tempPostId = post.id,
                imagePath = imagePath,
                transform = transform,
                caption = post.caption
            )
        )
    }
}