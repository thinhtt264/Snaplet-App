package com.thinh.snaplet.data.repository

import android.content.ContentValues
import android.content.Context
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import com.thinh.snaplet.data.datasource.remote.ApiService
import com.thinh.snaplet.data.model.CreatePostRequest
import com.thinh.snaplet.data.model.Post
import com.thinh.snaplet.data.model.PostsFeedData
import com.thinh.snaplet.data.model.media.ConfirmUploadData
import com.thinh.snaplet.data.model.media.MediaConfirmUploadRequest
import com.thinh.snaplet.data.model.media.ImageTransform
import com.thinh.snaplet.data.model.media.RequestUploadRequest
import com.thinh.snaplet.data.model.media.UploadRequestData
import com.thinh.snaplet.data.model.media.UploadRequestItem
import com.thinh.snaplet.di.BaseOkHttpClient
import com.thinh.snaplet.utils.Logger
import dagger.hilt.android.qualifiers.ApplicationContext
import com.thinh.snaplet.utils.network.ApiError
import com.thinh.snaplet.utils.network.ApiResult
import com.thinh.snaplet.utils.network.safeApiCall
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.InputStream
import javax.inject.Inject

class MediaRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val apiService: ApiService,
    @BaseOkHttpClient private val baseOkHttpClient: OkHttpClient,
) : MediaRepository {

    private companion object {
        const val DOWNLOAD_BUFFER_SIZE = 64 * 1024
    }

    override suspend fun downloadImage(imageSource: String): Result<String> =
        withContext(Dispatchers.IO) {
            if (imageSource.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("Image source is empty"))
            }
            try {
                val inputStream: InputStream = when {
                    imageSource.startsWith("http://", ignoreCase = true) ||
                    imageSource.startsWith("https://", ignoreCase = true) -> {
                        val response = baseOkHttpClient.newCall(
                            Request.Builder().url(imageSource).build()
                        ).execute()
                        if (!response.isSuccessful || response.body == null) {
                            return@withContext Result.failure(
                                RuntimeException("Download failed: ${response.code}")
                            )
                        }
                        response.body!!.byteStream()
                    }
                    else -> {
                        val path = imageSource.removePrefix("file://")
                        val file = File(path)
                        if (!file.exists() || !file.canRead()) {
                            return@withContext Result.failure(
                                RuntimeException("File not found or not readable")
                            )
                        }
                        file.inputStream()
                    }
                }
                val displayName = "Snaplet_${System.currentTimeMillis()}.jpg"
                val mimeType = "image/jpeg"
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
                    put(MediaStore.Images.Media.MIME_TYPE, mimeType)
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Snaplet")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
                val uri = context.contentResolver.insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    values
                ) ?: run {
                    inputStream.close()
                    return@withContext Result.failure(
                        RuntimeException("Failed to create file in MediaStore")
                    )
                }
                try {
                    context.contentResolver.openOutputStream(uri)?.use { rawOut ->
                        BufferedOutputStream(rawOut, DOWNLOAD_BUFFER_SIZE).use { out ->
                            BufferedInputStream(inputStream, DOWNLOAD_BUFFER_SIZE).use { input ->
                                input.copyTo(out, DOWNLOAD_BUFFER_SIZE)
                            }
                        }
                    }
                    values.clear()
                    values.put(MediaStore.Images.Media.IS_PENDING, 0)
                    context.contentResolver.update(uri, values, null, null)
                    Result.success(displayName)
                } finally {
                    inputStream.close()
                }
            } catch (e: Exception) {
                Logger.e(e, "downloadImage failed")
                Result.failure(e)
            }
        }

    override suspend fun getNewsfeed(limit: Int, cursor: String?): ApiResult<PostsFeedData> {
        return safeApiCall(
            apiCall = {
                apiService.getPostsFeed(limit = limit, cursor = cursor)
            }
        )
    }

    override suspend fun requestUpload(
        items: List<String>,
        transforms: List<ImageTransform>?
    ): ApiResult<UploadRequestData> {
        if (items.size > 3) {
            return ApiResult.Failure(
                ApiError(
                    httpCode = 400,
                    message = "Maximum 3 items allowed"
                )
            )
        }

        if (transforms != null && transforms.size != items.size) {
            return ApiResult.Failure(
                ApiError(
                    httpCode = 400,
                    message = "Transforms count must match items count"
                )
            )
        }

        val uploadRequestItems = mutableListOf<UploadRequestItem>()
        items.forEachIndexed { index, filePath ->
            val file = File(filePath)
            if (!file.exists()) {
                return ApiResult.Failure(
                    ApiError(
                        httpCode = 400,
                        message = "File does not exist: $filePath"
                    )
                )
            }

            val extension = file.extension.lowercase()
            val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
                ?: return ApiResult.Failure(
                    ApiError(
                        httpCode = 400,
                        message = "Cannot determine mime type for file: $filePath"
                    )
                )

            val fileSize = file.length()
            if (fileSize == 0L) {
                return ApiResult.Failure(
                    ApiError(
                        httpCode = 400,
                        message = "File is empty: $filePath"
                    )
                )
            }

            val transform = transforms?.get(index)
            uploadRequestItems.add(
                UploadRequestItem(
                    mimeType = mimeType,
                    size = fileSize,
                    transform = transform
                )
            )
        }

        Logger.d("📤 Requesting upload URLs for ${items.size} item(s)")
        return safeApiCall(
            apiCall = {
                apiService.requestUpload(RequestUploadRequest(items = uploadRequestItems))
            }
        )
    }

    override suspend fun uploadMedia(uploadUrl: String, filePath: String): ApiResult<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val file = File(filePath)
                if (!file.exists()) {
                    return@withContext ApiResult.Failure(
                        ApiError(
                            httpCode = 400,
                            message = "File does not exist: $filePath"
                        )
                    )
                }

                val extension = file.extension.lowercase()
                val contentType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
                    ?: return@withContext ApiResult.Failure(
                        ApiError(
                            httpCode = 400,
                            message = "Cannot determine content type for file: $filePath"
                        )
                    )

                val requestBody = file.asRequestBody(contentType.toMediaType())

                val request = Request.Builder()
                    .url(uploadUrl)
                    .put(requestBody)
                    .addHeader("Content-Type", contentType)
                    .build()

                val response = baseOkHttpClient.newCall(request).execute()

                if (response.isSuccessful) {
                    ApiResult.Success(Unit)
                } else {
                    ApiResult.Failure(
                        ApiError(
                            httpCode = response.code,
                            message = "Upload failed: ${response.message}"
                        )
                    )
                }
            } catch (e: Exception) {
                ApiResult.Failure(
                    ApiError(
                        httpCode = 500,
                        message = "Upload failed: ${e.message ?: "Unknown error"}"
                    )
                )
            }
        }
    }

    override suspend fun confirmUpload(mediaIds: List<String>): ApiResult<ConfirmUploadData> {
        if (mediaIds.size > 3) {
            return ApiResult.Failure(
                ApiError(
                    httpCode = 400,
                    message = "Maximum 3 media IDs allowed"
                )
            )
        }

        return safeApiCall(
            apiCall = {
                apiService.confirmUpload(MediaConfirmUploadRequest(mediaIds = mediaIds))
            }
        )
    }

    override suspend fun createPost(
        mediaIds: List<String>,
        caption: String?,
        visibility: String
    ): ApiResult<Post> {
        if (mediaIds.isEmpty()) {
            return ApiResult.Failure(
                ApiError(
                    httpCode = 400,
                    message = "At least one media ID is required"
                )
            )
        }

        if (mediaIds.size > 3) {
            return ApiResult.Failure(
                ApiError(
                    httpCode = 400,
                    message = "Maximum 3 media IDs allowed"
                )
            )
        }

        return safeApiCall(
            apiCall = {
                apiService.createPost(
                    CreatePostRequest(
                        mediaIds = mediaIds,
                        caption = caption,
                        visibility = visibility
                    )
                )
            }
        )
    }

    override suspend fun deletePost(postId: String): ApiResult<Unit> {
        return safeApiCall(
            apiCall = { apiService.deletePost(postId) },
        )
    }
}