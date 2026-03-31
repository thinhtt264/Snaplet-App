package com.thinh.snaplet.data.model.post

import com.google.gson.annotations.SerializedName
import com.thinh.snaplet.data.model.user.AvatarUrls
import java.util.Date

data class PostReactionUser(
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

    @SerializedName("reactionIcon")
    val reactionIcon: String,

    @SerializedName("reactedAt")
    val reactedAt: Date,
)