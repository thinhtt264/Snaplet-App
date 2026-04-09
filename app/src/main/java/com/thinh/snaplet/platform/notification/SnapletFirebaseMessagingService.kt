package com.thinh.snaplet.platform.notification

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.thinh.snaplet.domain.notification.PushNotificationType
import com.thinh.snaplet.domain.notification.RegisterFcmTokenUseCase
import com.thinh.snaplet.platform.widget.WidgetUpdateManager
import com.thinh.snaplet.utils.Logger
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

    @Inject
    lateinit var widgetUpdateManager: WidgetUpdateManager

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        scope.launch {
            registerFcmTokenUseCase(token)
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        scope.launch {
            val data = message.data
            val pushType = PushNotificationType.from(data[NotificationHelper.KEY_TYPE])
            when (pushType) {
                PushNotificationType.POST_REACTION -> {
                    val postId = data[NotificationHelper.KEY_POST_ID] ?: return@launch
                    val actorAvatarUrl = data[NotificationHelper.KEY_ACTOR_AVATAR_URL]
                    val title = message.notification?.title ?: data[NotificationHelper.KEY_TITLE]
                    val body = message.notification?.body ?: data[NotificationHelper.KEY_BODY]

                    if (!title.isNullOrBlank() && !body.isNullOrBlank()) {
                        notificationHelper.showReactionNotification(
                            title = title,
                            body = body,
                            postId = postId,
                            actorAvatarUrl = actorAvatarUrl,
                        )
                    }

                    widgetUpdateManager.scheduleImmediateUpdate(requireGlanceIds = true)
                }

                PushNotificationType.WIDGET_REFRESH -> {
                    widgetUpdateManager.scheduleImmediateUpdate(requireGlanceIds = true)
                }

                else -> {
                    Logger.d("FCM type ignored in service: %s", pushType.name)
                }
            }
        }
    }
}
