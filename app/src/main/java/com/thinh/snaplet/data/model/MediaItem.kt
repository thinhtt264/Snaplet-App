package com.thinh.snaplet.data.model

import com.google.gson.annotations.SerializedName

/**
 * Media Item Model
 *
 * Used for both API parsing and UI display Simpler approach:
 * Use @SerializedName directly
 */
sealed class MediaItem {
    abstract val id: String
    abstract val url: String

    /** Photo media item with full post information */
    data class Photo(
        @SerializedName("id")
        override val id: String,

        @SerializedName("userId")
        val userId: String,

        @SerializedName("username")
        val username: String,

        @SerializedName("displayName")
        val displayName: String,

        @SerializedName("avatarUrl")
        val avatarUrl: String? = null,

        @SerializedName("imageUrl")
        override val url: String,

        @SerializedName("caption")
        val caption: String? = null,

        @SerializedName("visibility")
        val visibility: String,

        @SerializedName("createdAt")
        val createdAt: String,

        @SerializedName("isOwnPost")
        val isOwnPost: Boolean,

        ) : MediaItem()

    /** Video media item (prepared for future use) */
    data class Video(
        @SerializedName("id")
        override val id: String,

        @SerializedName("video_url")
        override val url: String,

        @SerializedName("thumbnail_url")
        val thumbnailUrl: String,

        @SerializedName("duration")
        val durationMs: Long
    ) : MediaItem()
}
