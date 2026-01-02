package com.thinh.snaplet.data.repository.auth

import AuthState
import com.thinh.snaplet.data.datasource.local.datastore.DataStoreManager
import com.thinh.snaplet.data.datasource.remote.ApiService
import com.thinh.snaplet.data.model.LoginRequest
import com.thinh.snaplet.data.model.UserProfile
import com.thinh.snaplet.utils.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val dataStoreManager: DataStoreManager
) : AuthRepository {

    private val _authState =
        MutableStateFlow<AuthState>(AuthState.Unauthenticated)

    override val authState: StateFlow<AuthState> = _authState

    override suspend fun login(email: String, password: String): Result<UserProfile> {
        return try {
            val request = LoginRequest(email = email, password = password)
            val response = apiService.login(body = request)

            if (response.isSuccessful) {
                val body = response.body()

                if (body == null || body.status.code != 200) {
                    return Result.failure(Exception("Login failed"))
                }

                val result = body.data

                dataStoreManager.saveTokens(
                    accessToken = result.token.accessToken,
                    refreshToken = result.token.refreshToken
                )
                dataStoreManager.saveUserProfile(result.user)

                Result.success(result.user)
            } else {
                val errorMsg = "HTTP ${response.code()}: ${response.message()}"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Logger.e("❌ Failed to login: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun logout() {
        dataStoreManager.clearSession()
        _authState.value = AuthState.Unauthenticated
    }

    override suspend fun isAuthenticated(): Boolean {
        val authenticated =
            dataStoreManager.getAccessToken() != null &&
                    dataStoreManager.getRefreshToken() != null &&
                    dataStoreManager.getUserProfile() != null

        _authState.value =
            if (authenticated)
                AuthState.Authenticated
            else
                AuthState.Unauthenticated

        return authenticated
    }
}