package com.thinh.snaplet.data.model

import com.google.gson.annotations.SerializedName
import com.thinh.snaplet.data.model.media.Media
import com.thinh.snaplet.data.model.user.AvatarUrls

data class Post(
    @SerializedName("id")
    val id: String,

    @SerializedName("userId")
    val userId: String,

    @SerializedName("username")
    val username: String,

    @SerializedName("firstName")
    val firstName: String,

    @SerializedName("lastName")
    val lastName: String,

    @SerializedName("avatarUrls")
    val avatarUrls: AvatarUrls = AvatarUrls(),

    @SerializedName("media")
    val media: List<Media> = emptyList(),

    @SerializedName("caption")
    val caption: String? = null,

    @SerializedName("visibility")
    val visibility: String,

    @SerializedName("createdAt")
    val createdAt: String,

    @SerializedName("isOwnPost")
    val isOwnPost: Boolean,
) {
    val displayName: String
        get() = "$firstName $lastName"
}

data class CreatePostRequest(
    @SerializedName("mediaIds")
    val mediaIds: List<String>,

    @SerializedName("caption")
    val caption: String? = null,

    @SerializedName("visibility")
    val visibility: String
)

typealias PostsFeedData = PaginatedResponse<Post>

data class UnreadPostsCountData(
    @SerializedName("count")
    val count: Int
)

data class MarkPostsSeenRequest(
    @SerializedName("lastSeenPostCreatedAt")
    val lastSeenPostCreatedAt: String
)
