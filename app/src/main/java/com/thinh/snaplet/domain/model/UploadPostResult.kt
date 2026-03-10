package com.thinh.snaplet.domain.model

import com.thinh.snaplet.data.model.Post

sealed class UploadPostResult {
    data class Success(val post: Post) : UploadPostResult()
    data class Failed(val message: String) : UploadPostResult()
}