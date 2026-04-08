package com.thinh.snaplet.platform.notification

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.thinh.snaplet.domain.notification.RegisterFcmTokenUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SnapletFirebaseMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var registerFcmTokenUseCase: RegisterFcmTokenUseCase

    @Inject
    lateinit var notificationHelper: NotificationHelper

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        scope.launch {
            registerFcmTokenUseCase(token)
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val data = message.data
        val postId = data["postId"] ?: return

        val title = message.notification?.title ?: data["title"] ?: return
        val body = message.notification?.body ?: data["body"] ?: return

        notificationHelper.showReactionNotification(
            title = title,
            body = body,
            postId = postId,
        )
    }
}
