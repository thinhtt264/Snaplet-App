package com.thinh.snaplet.network

import AuthState
import com.thinh.snaplet.data.repository.auth.AuthRepository
import com.thinh.snaplet.utils.Logger
import dagger.Lazy
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenAuthenticator @Inject constructor(
    private val tokenRefreshCoordinator: TokenRefreshCoordinator,
    private val authRepository: Lazy<AuthRepository>
) : Authenticator {

    companion object {
        private const val MAX_RETRY_ATTEMPTS = 2
        private const val REFRESH_TOKEN_ENDPOINT = "auth/refresh"
    }

    override fun authenticate(route: Route?, response: Response): Request? {
        if (tokenRefreshCoordinator.isForceLogoutTriggered()) {
            return null
        }

        val requestUrl = response.request.url.toString()

        if (requestUrl.contains(REFRESH_TOKEN_ENDPOINT)) {
            return null
        }

        val retryCount = responseCount(response)
        if (retryCount >= MAX_RETRY_ATTEMPTS) {
            Logger.e("❌ Max retry attempts ($MAX_RETRY_ATTEMPTS) reached, giving up")
            tokenRefreshCoordinator.forceLogout()
            return null
        }

        if (authRepository.get().authState.value != AuthState.Authenticated) {
            return null
        }

        Logger.d("🔄 Authenticator triggered for 401 response (attempt $retryCount)")

        val newAccessToken = runBlocking {
            tokenRefreshCoordinator.getNewAccessToken()
        }

        return if (newAccessToken != null) {
            Logger.d("✅ Retrying request with new access token")
            response.request.newBuilder().header("Authorization", "Bearer $newAccessToken").build()
        } else {
            null
        }
    }

    private fun responseCount(response: Response): Int {
        var result = 1
        var priorResponse = response.priorResponse
        while (priorResponse != null) {
            result++
            priorResponse = priorResponse.priorResponse
        }
        return result
    }
}