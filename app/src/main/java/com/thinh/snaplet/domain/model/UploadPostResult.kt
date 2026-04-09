package com.thinh.snaplet.domain.model

import com.thinh.snaplet.data.model.post.Post
import com.thinh.snaplet.utils.network.ApiError

sealed class UploadPostResult {
    data class Success(val post: Post) : UploadPostResult()
    data class Failed(
        val message: String,
        val apiError: ApiError? = null,
    ) : UploadPostResult()
}