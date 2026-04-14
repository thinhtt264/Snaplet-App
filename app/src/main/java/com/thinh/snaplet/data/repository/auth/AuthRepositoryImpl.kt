package com.thinh.snaplet.data.repository.auth

import AuthState
import com.thinh.snaplet.data.datasource.local.datastore.DataStoreManager
import com.thinh.snaplet.data.datasource.remote.ApiService
import com.thinh.snaplet.data.model.CompleteOnboardRequest
import com.thinh.snaplet.data.model.LoginRequest
import com.thinh.snaplet.data.model.LoginResponse
import com.thinh.snaplet.data.model.LoginWithGoogleRequest
import com.thinh.snaplet.data.model.RefreshTokenRequest
import com.thinh.snaplet.data.model.RegisterRequest
import com.thinh.snaplet.data.model.TokenResponse
import com.thinh.snaplet.data.model.user.UserProfile
import com.thinh.snaplet.network.SessionController
import com.thinh.snaplet.utils.network.ApiError
import com.thinh.snaplet.utils.network.ApiResult
import com.thinh.snaplet.utils.network.safeApiCall
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val dataStoreManager: DataStoreManager,
    private val sessionController: SessionController,
) : AuthRepository {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
    private val logoutScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override val authState: StateFlow<AuthState> = _authState

    override suspend fun login(email: String, password: String): ApiResult<UserProfile> {
        dataStoreManager.clearSession()
        return safeApiCall(apiCall = {
            apiService.login(body = LoginRequest(email, password))
        }, onSuccess = { result ->
            dataStoreManager.saveTokens(
                result.token.accessToken, result.token.refreshToken
            )
            dataStoreManager.saveUserProfile(result.user)
            _authState.value = AuthState.Authenticated
            sessionController.onNewAuthenticatedSession()
        }, transform = { response -> response.user })
    }

    override suspend fun loginWithGoogle(
        idToken: String,
    ): ApiResult<LoginResponse> {
        return safeApiCall(apiCall = {
            apiService.loginWithGoogle(
                body = LoginWithGoogleRequest(
                    idToken,
                )
            )
        }, onSuccess = { result ->
            dataStoreManager.saveTokens(
                result.token.accessToken, result.token.refreshToken
            )
            dataStoreManager.saveUserProfile(result.user)
            dataStoreManager.saveCompleteOnboarding(!result.requiresOnboarding)

            if (!result.requiresOnboarding) {
                _authState.value = AuthState.Authenticated
                sessionController.onNewAuthenticatedSession()
            }
        })
    }

    override suspend fun completeOnboarding(
        username: String,
        firstName: String,
        lastName: String
    ): ApiResult<UserProfile> {
        return safeApiCall(apiCall = {
            apiService.completeOnboarding(
                body = CompleteOnboardRequest(
                    username,
                    firstName,
                    lastName
                )
            )
        }, onSuccess = { result ->
            dataStoreManager.saveUserProfile(result)
        })
    }

    override suspend fun register(
        email: String, username: String, firstName: String, lastName: String, password: String
    ): ApiResult<UserProfile> {
        dataStoreManager.clearSession()

        val request = RegisterRequest(
            email = email,
            username = username,
            firstName = firstName,
            lastName = lastName,
            password = password
        )

        return safeApiCall(apiCall = {
            apiService.register(body = request)
        }, onSuccess = { result ->
            dataStoreManager.saveTokens(
                accessToken = result.token.accessToken, refreshToken = result.token.refreshToken
            )
            dataStoreManager.saveUserProfile(result.user)
        }, transform = { response -> response.user })
    }

    override suspend fun activatePendingRegistrationSession() {
        if (_authState.value is AuthState.Authenticated) return
        _authState.value = AuthState.Authenticated
        sessionController.onNewAuthenticatedSession()
    }

    override suspend fun logout() {
        val accessToken = dataStoreManager.getAccessToken()

        _authState.value = AuthState.Unauthenticated
        dataStoreManager.clearSession()

        logoutScope.launch {
            if (accessToken.isNullOrBlank()) return@launch
            safeApiCall(apiCall = { apiService.logout("Bearer $accessToken") })
        }
    }

    override suspend fun forceLogout() {
        dataStoreManager.clearSession()
        _authState.value = AuthState.Unauthenticated
    }

    override suspend fun isAuthenticated(): Boolean {
        val isCompleteOnboarding = dataStoreManager.loadCompleteOnboarding()

        if (isCompleteOnboarding == false) {
            dataStoreManager.clearSession()
            _authState.value = AuthState.Unauthenticated
            return false
        }

        val authenticated =
            dataStoreManager.loadAccessToken() != null && dataStoreManager.loadRefreshToken() != null && dataStoreManager.loadUserProfile() != null

        _authState.value = if (authenticated) AuthState.Authenticated
        else AuthState.Unauthenticated

        return _authState.value is AuthState.Authenticated
    }

    override suspend fun checkEmailAvailability(email: String): ApiResult<Boolean> {
        return safeApiCall(apiCall = {
            apiService.checkEmailAvailability(email)
        }, transform = { response -> response.available })
    }

    override suspend fun checkUsernameAvailability(username: String): ApiResult<Boolean> {
        return safeApiCall(apiCall = {
            apiService.checkUsernameAvailability(username)
        }, transform = { response -> response.available })
    }

    override suspend fun refreshToken(): ApiResult<TokenResponse> {
        val accessToken = dataStoreManager.getAccessToken()
        val refreshToken = dataStoreManager.getRefreshToken()

        if (accessToken.isNullOrBlank() || refreshToken.isNullOrBlank()) {
            return ApiResult.Failure(
                ApiError(
                    httpCode = 401, message = "Access token or refresh token not found"
                )
            )
        }

        return safeApiCall(apiCall = {
            apiService.refreshToken(
                RefreshTokenRequest(
                    refreshToken = refreshToken, accessToken = accessToken
                )
            )
        }, onSuccess = { result ->
            dataStoreManager.saveTokens(
                accessToken = result.accessToken, refreshToken = result.refreshToken
            )
        })
    }

    override suspend fun getAccessToken(): String? {
        return dataStoreManager.getAccessToken() ?: dataStoreManager.loadAccessToken()
    }
}