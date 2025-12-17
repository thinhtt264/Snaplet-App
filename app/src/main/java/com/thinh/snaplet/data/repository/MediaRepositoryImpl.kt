package com.thinh.snaplet.data.repository

import android.net.Uri
import com.thinh.snaplet.data.datasource.remote.ApiService
import com.thinh.snaplet.data.model.MediaItem
import com.thinh.snaplet.utils.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject

class MediaRepositoryImpl @Inject constructor(
    private val apiService: ApiService
) : MediaRepository {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)

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

            // Check status code
            if (body.status.code != 200) {
                Logger.e("❌ API error: ${body.status.message}")
                return@withContext Result.failure(Exception(body.status.message))
            }

            // Get media items directly from response
            val mediaItems = body.data.data.map { photo ->
                // Parse timestamp from createdAt string
                val timestamp = try {
                    dateFormat.parse(photo.createdAt)?.time ?: System.currentTimeMillis()
                } catch (e: Exception) {
                    Logger.e(e, "Failed to parse date: ${photo.createdAt}")
                    System.currentTimeMillis()
                }

                // Return photo with parsed timestamp
                photo.copy(timestamp = timestamp)
            }
            Result.success(mediaItems)
        } catch (e: Exception) {
            Logger.e(e, "❌ Failed to fetch media feed")
            Result.failure(e)
        }
    }

    /** Upload photo to server */
    override suspend fun uploadPhoto(uri: Uri): Result<MediaItem> = withContext(Dispatchers.IO) {
        try {
            Logger.d("📤 Uploading photo: $uri")

            // TODO: Implement upload logic when API is ready
            Result.failure(Exception("Upload not implemented yet"))

        } catch (e: Exception) {
            Logger.e(e, "❌ Upload failed")
            Result.failure(e)
        }
    }

    /** Load more media for pagination */
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
