package com.thinh.snaplet.data.model.user

import com.google.gson.annotations.SerializedName

data class UserSearchResult(
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

    // Relationship fields (may be null if there is no relationship between users).
    @SerializedName("id")
    val relationshipId: String? = null,

    @SerializedName("status")
    val relationshipStatus: String? = null,

    @SerializedName("createdAt")
    val relationshipCreatedAt: String? = null,
)