package com.thinh.snaplet.data.model.post

import com.google.gson.annotations.SerializedName

data class PostActivity(
    @SerializedName("imageUrl")
    val imageUrl: String,
    @SerializedName("caption")
    val caption: String?,
    @SerializedName("senderAvatarUrl")
    val senderAvatarUrl: String?,
    @SerializedName("unreadCount")
    val unreadCount: Int,
)
