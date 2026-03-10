package com.thinh.snaplet.data.repository.auth

import AuthState
import com.thinh.snaplet.data.model.TokenResponse
import com.thinh.snaplet.data.model.user.UserProfile
import com.thinh.snaplet.utils.network.ApiResult
import kotlinx.coroutines.flow.StateFlow

interface AuthRepository {
    val authState: StateFlow<AuthState>

    suspend fun login(email: String, password: String): ApiResult<UserProfile>

    suspend fun register(
        email: String,
        username: String,
        firstName: String,
        lastName: String,
        password: String
    ): ApiResult<UserProfile>

    suspend fun logout()

    suspend fun forceLogout()

    suspend fun isAuthenticated(): Boolean

    suspend fun checkEmailAvailability(email: String): ApiResult<Boolean>

    suspend fun checkUsernameAvailability(username: String): ApiResult<Boolean>

    suspend fun refreshToken(): ApiResult<TokenResponse>
}