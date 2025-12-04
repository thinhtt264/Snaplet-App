package com.thinh.snaplet.data.model

import com.google.gson.annotations.SerializedName

/**
 * Media Item Model
 * 
 * Used for both API parsing and UI display
 * Simpler approach: Use @SerializedName directly
 */
sealed class MediaItem {
    abstract val id: String
    abstract val url: String
    abstract val timestamp: Long
    
    /**
     * Photo media item
     */
    data class Photo(
        @SerializedName("id")
        override val id: String,
        
        @SerializedName("image_url")
        override val url: String,
        
        @SerializedName("created_at")
        override val timestamp: Long = System.currentTimeMillis()
    ) : MediaItem()
    
    /**
     * Video media item (prepared for future use)
     */
    data class Video(
        @SerializedName("id")
        override val id: String,
        
        @SerializedName("video_url")
        override val url: String,
        
        @SerializedName("created_at")
        override val timestamp: Long = System.currentTimeMillis(),
        
        @SerializedName("thumbnail_url")
        val thumbnailUrl: String,
        
        @SerializedName("duration")
        val durationMs: Long
    ) : MediaItem()
}
