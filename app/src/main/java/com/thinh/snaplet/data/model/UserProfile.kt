package com.thinh.snaplet.data.model

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
    
    @SerializedName("avatarUrl")
    val avatarUrl: String? = null,
    
    @SerializedName("email")
    val email: String,
    
    @SerializedName("createdAt")
    val createdAt: String
) {
    val displayName: String
        get() = "$firstName $lastName".trim()
}

