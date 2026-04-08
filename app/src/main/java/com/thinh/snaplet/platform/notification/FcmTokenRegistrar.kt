package com.thinh.snaplet.platform.notification

import com.google.firebase.messaging.FirebaseMessaging
import com.thinh.snaplet.domain.notification.RegisterFcmTokenUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Registers the current FCM token with the backend after Firebase has a token (e.g. post-login).
 * Keeps Firebase SDK usage out of ViewModels.
 */
@Singleton
class FcmTokenRegistrar @Inject constructor(
    private val registerFcmTokenUseCase: RegisterFcmTokenUseCase,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun syncCurrentTokenToBackend() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) return@addOnCompleteListener
            val token = task.result ?: return@addOnCompleteListener
            scope.launch {
                registerFcmTokenUseCase(token)
            }
        }
    }
}
