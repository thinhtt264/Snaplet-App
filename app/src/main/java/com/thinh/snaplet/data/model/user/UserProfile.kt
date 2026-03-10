package com.thinh.snaplet.data.model.user

import com.google.gson.annotations.SerializedName

data class UserProfile(
    @SerializedName("id")
    val id: String,

    @SerializedName("username")
    val userName: String,

    @SerializedName("firstName")
    val firstName: String,

    @SerializedName("lastName")
    val lastName: String,

    @SerializedName("avatarUrls")
    val avatarUrls: AvatarUrls = AvatarUrls(),

    @SerializedName("email")
    val email: String
) {
    val displayName: String
        get() = "$firstName $lastName".trim()
}

