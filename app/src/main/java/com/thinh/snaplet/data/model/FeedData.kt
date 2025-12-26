package com.thinh.snaplet.data.model

import com.google.gson.annotations.SerializedName

/**
 * Feed response containing media items and pagination
 */
data class FeedData(
    @SerializedName("data")
    val data: List<MediaItem.Photo>,
    
    @SerializedName("pagination")
    val pagination: Pagination
)

