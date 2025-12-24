package com.thinh.snaplet.data.model

import com.google.gson.annotations.SerializedName

/**
 * Standard API Response Structure
 */
data class ResponseStatus(
    @SerializedName("code")
    val code: Int,
    
    @SerializedName("message")
    val message: String
)

data class StandardResponse<T>(
    @SerializedName("status")
    val status: ResponseStatus,
    
    @SerializedName("data")
    val data: T
)

/**
 * Pagination metadata
 */
data class Pagination(
    @SerializedName("offset")
    val offset: Int,
    
    @SerializedName("limit")
    val limit: Int,
)

/**
 * Feed response containing media items and pagination
 */
data class FeedData(
    @SerializedName("data")
    val data: List<MediaItem.Photo>,
    
    @SerializedName("pagination")
    val pagination: Pagination
)

