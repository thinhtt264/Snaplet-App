package com.thinh.snaplet.data.model

import com.google.gson.annotations.SerializedName
import com.thinh.snaplet.data.model.user.UserProfile

data class LoginRequest(
    @SerializedName("email")
    val email: String,
    
    @SerializedName("password")
    val password: String
)

data class RegisterRequest(
    @SerializedName("email")
    val email: String,
    
    @SerializedName("username")
    val username: String,
    
    @SerializedName("firstName")
    val firstName: String,
    
    @SerializedName("lastName")
    val lastName: String,
    
    @SerializedName("password")
    val password: String
)

data class TokenResponse(
    @SerializedName("accessToken")
    val accessToken: String,
    
    @SerializedName("refreshToken")
    val refreshToken: String
)

data class LoginResponse(
    @SerializedName("token")
    val token: TokenResponse,
    
    @SerializedName("user")
    val user: UserProfile
)

data class EmailAvailabilityData(
    @SerializedName("available")
    val available: Boolean
)

data class UsernameAvailabilityData(
    @SerializedName("available")
    val available: Boolean
)

data class RefreshTokenRequest(
    @SerializedName("refreshToken")
    val refreshToken: String,
    
    @SerializedName("accessToken")
    val accessToken: String
)
