package com.thinh.snaplet.data.repository

import android.net.Uri
import com.thinh.snaplet.data.model.MediaItem
import com.thinh.snaplet.data.datasource.remote.ApiService
import com.thinh.snaplet.utils.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Real implementation of MediaRepository with actual API calls
 * 
 * Simplified approach: No DTO/Mapper layer
 * API returns MediaItem directly via @SerializedName
 */
class MediaRepositoryImpl @Inject constructor(
    private val apiService: ApiService
) : MediaRepository {
    
    /**
     * Get media feed from API
     * Direct conversion via @SerializedName
     */
    override suspend fun getMediaFeed(): Result<List<MediaItem>> = withContext(Dispatchers.IO) {
        try {
            Logger.d("📡 Fetching media feed from API...")
            
            val response = apiService.getMediaFeed(page = 1, perPage = 20)
            
            if (!response.isSuccessful) {
                val errorMsg = "API returned ${response.code()}: ${response.message()}"
                Logger.e("❌ $errorMsg")
                return@withContext Result.failure(Exception(errorMsg))
            }
            
            val mediaItems = response.body() ?: emptyList()
            
            Logger.d("✅ Loaded ${mediaItems.size} media items from API")
            Result.success(mediaItems)
            
        } catch (e: Exception) {
            Logger.e(e, "❌ Failed to fetch media feed")
            Result.failure(e)
        }
    }
    
    /**
     * Upload photo to server
     */
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
    
    /**
     * Load more media for pagination
     */
    override suspend fun loadMoreMedia(lastId: String): Result<List<MediaItem>> = withContext(Dispatchers.IO) {
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
