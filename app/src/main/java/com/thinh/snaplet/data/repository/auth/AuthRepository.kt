package com.thinh.snaplet.data.repository.auth

import AuthState
import com.thinh.snaplet.data.model.UserProfile
import kotlinx.coroutines.flow.StateFlow

interface AuthRepository {
    val authState: StateFlow<AuthState>

    suspend fun login(email: String, password: String): Result<UserProfile>

    suspend fun logout()

    suspend fun isAuthenticated(): Boolean
}