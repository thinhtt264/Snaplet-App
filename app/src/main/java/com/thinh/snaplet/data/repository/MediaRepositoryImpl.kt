package com.thinh.snaplet.data.repository

import android.net.Uri
import com.thinh.snaplet.data.datasource.remote.ApiService
import com.thinh.snaplet.data.model.MediaItem
import com.thinh.snaplet.utils.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class MediaRepositoryImpl @Inject constructor(
    private val apiService: ApiService
) : MediaRepository {

    override suspend fun getMediaFeed(): Result<List<MediaItem>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getMediaFeed(limit = 10, offset = 0)

            if (!response.isSuccessful) {
                val errorMsg = "API returned ${response.code()}: ${response.message()}"
                Logger.e("❌ $errorMsg")
                return@withContext Result.failure(Exception(errorMsg))
            }

            val body = response.body()
            if (body == null) {
                Logger.e("❌ Response body is null")
                return@withContext Result.failure(Exception("Empty response from server"))
            }

            if (body.status.code != 200) {
                Logger.e("❌ API error: ${body.status.message}")
                return@withContext Result.failure(Exception(body.status.message))
            }

            val mediaItems = body.data.data.map { photo -> photo.copy() }
            Result.success(mediaItems)
        } catch (e: Exception) {
            Logger.e(e, "❌ Failed to fetch media feed")
            Result.failure(e)
        }
    }

    override suspend fun uploadPhoto(uri: Uri): Result<MediaItem> = withContext(Dispatchers.IO) {
        try {
            Logger.d("📤 Uploading photo: $uri")

            Result.failure(Exception("Upload not implemented yet"))

        } catch (e: Exception) {
            Logger.e(e, "❌ Upload failed")
            Result.failure(e)
        }
    }

    override suspend fun loadMoreMedia(lastId: String): Result<List<MediaItem>> =
        withContext(Dispatchers.IO) {
            try {
                Logger.d("📡 Loading more media after: $lastId")

                // TODO: Implement pagination when API is ready
                Result.success(emptyList())

            } catch (e: Exception) {
                Logger.e(e, "❌ Failed to load more media")
                Result.failure(e)
            }
        }
}
