package com.thinh.snaplet.data.repository

import android.net.Uri
import com.thinh.snaplet.data.model.MediaItem

/**
 * Repository interface for media operations
 * This abstraction allows easy switching between fake and real implementations
 */
interface MediaRepository {
    
    /**
     * Get media feed
     * @return Result with list of media items or error
     */
    suspend fun getMediaFeed(): Result<List<MediaItem>>
    
    /**
     * Upload new photo (prepared for future implementation)
     */
    suspend fun uploadPhoto(uri: Uri): Result<MediaItem>
    
    /**
     * Load more media for pagination (prepared for future implementation)
     */
    suspend fun loadMoreMedia(lastId: String): Result<List<MediaItem>>
}
