package com.thinh.snaplet.data.repository

import com.thinh.snaplet.data.model.media.ConfirmUploadData
import com.thinh.snaplet.data.model.media.UploadRequestData
import com.thinh.snaplet.data.model.post.Post
import com.thinh.snaplet.utils.network.ApiResult

interface MediaRepository {

    suspend fun downloadImage(imageSource: String): Result<String>

    suspend fun prepareShareImageUri(imageSource: String): Result<android.net.Uri>

    suspend fun requestUpload(
        items: List<String>,
        transforms: List<com.thinh.snaplet.data.model.media.ImageTransform>? = null
    ): ApiResult<UploadRequestData>

    suspend fun uploadMedia(uploadUrl: String, filePath: String): ApiResult<Unit>

    suspend fun confirmUpload(mediaIds: List<String>): ApiResult<ConfirmUploadData>

    suspend fun createPost(
        mediaIds: List<String>,
        caption: String? = null,
        visibility: String
    ): ApiResult<Post>

    suspend fun deletePost(postId: String): ApiResult<Unit>
}
