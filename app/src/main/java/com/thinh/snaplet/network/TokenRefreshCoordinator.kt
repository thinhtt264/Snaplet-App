package com.thinh.snaplet.network

import com.thinh.snaplet.data.repository.auth.AuthRepository
import com.thinh.snaplet.ui.overlay.ModalContent
import com.thinh.snaplet.ui.overlay.OverlayEventBus
import com.thinh.snaplet.utils.Logger
import dagger.Lazy
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenRefreshCoordinator @Inject constructor(
    private val authRepository: Lazy<AuthRepository>,
) : SessionController {

    private val mutex = Mutex()
    private var ongoingRefresh: CompletableDeferred<String?>? = null
    private val forceLogoutMutex = Mutex()

    private val forceLogoutTriggered = AtomicBoolean(false)

    fun isForceLogoutTriggered(): Boolean = forceLogoutTriggered.get()

    override fun onNewAuthenticatedSession() {
        forceLogoutTriggered.set(false)
    }

    suspend fun getNewAccessToken(): String? {
        if (forceLogoutTriggered.get()) return null
        val (deferred, isOwner) = mutex.withLock {
            val existing = ongoingRefresh
            if (existing != null) {
                existing to false
            } else {
                val new = CompletableDeferred<String?>()
                ongoingRefresh = new
                new to true
            }
        }

        if (!isOwner) {
            Logger.d("⏳ Waiting for ongoing refresh...")
            return deferred.await()
        }

        return executeRefreshWithRetry(deferred)
    }

    private suspend fun executeRefreshWithRetry(
        deferred: CompletableDeferred<String?>,
        isRetry: Boolean = false
    ): String? {
        return try {
            if (forceLogoutTriggered.get()) return null
            val result = authRepository.get().refreshToken()

            val newToken = result.fold(
                onSuccess = { tokenResponse ->
                    tokenResponse.accessToken
                },
                onFailure = { error ->
                    when {
                        error.httpCode == 401 -> {
                            triggerForceLogoutFlow()
                            null
                        }

                        !isRetry -> {
                            Logger.w("⚠️ Refresh failed (${error.httpCode}), retrying")
                            return executeRefreshWithRetry(deferred, isRetry = true)
                        }

                        else -> {
                            triggerForceLogoutFlow()
                            null
                        }
                    }
                }
            )

            deferred.complete(newToken)
            newToken
        } catch (e: Exception) {
            if (!isRetry) {
                Logger.w("⚠️ Refresh exception: ${e.message}, retrying")
                return executeRefreshWithRetry(deferred, isRetry = true)
            }
            deferred.complete(null)
            triggerForceLogoutFlow()
            null
        } finally {
            mutex.withLock { ongoingRefresh = null }
        }
    }

    private suspend fun triggerForceLogoutFlow() {
        forceLogoutMutex.withLock {
            if (forceLogoutTriggered.getAndSet(true)) return@withLock

            OverlayEventBus.showModal(
                isBlocking = true,
                content = ModalContent.ForceLogoutDialog(
                    onConfirm = {
                        runBlocking { authRepository.get().forceLogout() }
                    },
                ),
            )
        }
    }

    fun forceLogout() {
        runBlocking { triggerForceLogoutFlow() }
    }
}
