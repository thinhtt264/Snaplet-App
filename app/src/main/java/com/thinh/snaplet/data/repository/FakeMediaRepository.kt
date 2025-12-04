package com.thinh.snaplet.data.repository

import android.net.Uri
import com.thinh.snaplet.data.model.MediaItem
import com.thinh.snaplet.utils.Logger
import kotlinx.coroutines.delay
import javax.inject.Inject

/**
 * Fake implementation for development
 * Will be replaced by MediaRepositoryImpl when API is ready
 * 
 * This allows:
 * - Frontend development without backend dependency
 * - Network delay simulation
 * - Easy testing
 */
class FakeMediaRepository @Inject constructor() : MediaRepository {
    
    // Mock photo URLs from Pexels
    private val mockUrls = listOf(
        "https://images.pexels.com/photos/1939485/pexels-photo-1939485.jpeg",
        "https://images.pexels.com/photos/531880/pexels-photo-531880.jpeg",
        "https://content.pexels.com/images/canva/ai-generated-ad/off-theme/hummingbird_hibiscus_moment-full.jpg",
        "https://content.pexels.com/images/canva/ai-generated-ad/off-theme/bmw_driving_on_open_road-full.jpg",
        "https://content.pexels.com/images/canva/ai-generated-ad/off-theme/crystal_clear_milky_way-full.jpg",
        "https://images.pexels.com/photos/255379/pexels-photo-255379.jpeg",
        "https://images.pexels.com/photos/303383/pexels-photo-303383.jpeg",
        "https://images.pexels.com/photos/673648/pexels-photo-673648.jpeg",
        "https://images.pexels.com/photos/268533/pexels-photo-268533.jpeg",
        "https://images.pexels.com/photos/164005/pexels-photo-164005.jpeg"
    )
    
    override suspend fun getMediaFeed(): Result<List<MediaItem>> {
        return try {
            Logger.d("🎭 FakeMediaRepository: getMediaFeed() called - DI is working!")
            
            // Simulate network delay for realistic behavior
            delay(800)
            
            // Convert mock URLs to MediaItem.Photo
            val photos = mockUrls.mapIndexed { index, url ->
                MediaItem.Photo(
                    id = "mock_photo_$index",
                    url = url,
                    timestamp = System.currentTimeMillis() - (index * 3600000L) // 1 hour apart
                )
            }
            
            Result.success(photos)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun uploadPhoto(uri: Uri): Result<MediaItem> {
        // Fake upload - simulate API response
        delay(1000)
        return Result.success(
            MediaItem.Photo(
                id = "uploaded_${System.currentTimeMillis()}",
                url = uri.toString(), // In real API: server returns CDN URL
                timestamp = System.currentTimeMillis()
            )
        )
    }
    
    override suspend fun loadMoreMedia(lastId: String): Result<List<MediaItem>> {
        // Fake pagination - no more data for now
        delay(500)
        return Result.success(emptyList())
    }
}
