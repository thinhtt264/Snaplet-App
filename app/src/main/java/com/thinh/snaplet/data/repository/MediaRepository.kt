package com.thinh.snaplet.data.repository

import android.net.Uri
import com.thinh.snaplet.data.model.MediaItem

interface MediaRepository {
    
    suspend fun getMediaFeed(): Result<List<MediaItem>>
    
    suspend fun uploadPhoto(uri: Uri): Result<MediaItem>
    
    suspend fun loadMoreMedia(lastId: String): Result<List<MediaItem>>
}
