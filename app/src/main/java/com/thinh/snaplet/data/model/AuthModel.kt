package com.thinh.snaplet.data.model

import com.google.gson.annotations.SerializedName

/**
 * Authentication Models
 */

/**
 * Login Request
 */
data class LoginRequest(
    @SerializedName("email")
    val email: String,
    
    @SerializedName("password")
    val password: String
)

/**
 * Token Response
 */
data class TokenResponse(
    @SerializedName("accessToken")
    val accessToken: String,
    
    @SerializedName("refreshToken")
    val refreshToken: String
)

/**
 * Login Response
 */
data class LoginResponse(
    @SerializedName("token")
    val token: TokenResponse,
    
    @SerializedName("user")
    val user: UserProfile
)

