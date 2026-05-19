package com.thinh.snaplet.platform.notification

import androidx.annotation.RequiresPermission
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

    @RequiresPermission(android.Manifest.permission.POST_NOTIFICATIONS)
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        scope.launch {
            val data = message.data
            val pushType = PushNotificationType.from(data[NotificationHelper.KEY_TYPE])
            when (pushType) {
                PushNotificationType.CUSTOM -> {
                    val deeplink = data[NotificationHelper.KEY_DEEPLINK] ?: ""
                    val largeIconUrl = data[NotificationHelper.KEY_LARGE_ICON_URL]
                    val title = message.notification?.title ?: data[NotificationHelper.KEY_TITLE]
                    val body = message.notification?.body ?: data[NotificationHelper.KEY_BODY]

                    if (!title.isNullOrBlank() && !body.isNullOrBlank()) {
                        notificationHelper.showCustomNotification(
                            title = title,
                            body = body,
                            deeplink = deeplink,
                            largeIconUrl = largeIconUrl,
                        )
                    }
                }

                PushNotificationType.WIDGET_REFRESH -> {
                    widgetUpdateManager.scheduleImmediateUpdate(requireGlanceIds = true)
                }

                PushNotificationType.NEW_CHAT_MESSAGE -> {
                    val conversationId =
                        data[NotificationHelper.KEY_CONVERSATION_ID] ?: return@launch
                    val messageId = data[NotificationHelper.KEY_MESSAGE_ID] ?: return@launch
                    val senderName = data[NotificationHelper.KEY_SENDER_NAME] ?: return@launch
                    val senderAvatarUrl = data[NotificationHelper.KEY_SENDER_AVATAR_URL]
                    val text = data[NotificationHelper.KEY_TEXT]
                    val hasImage =
                        data[NotificationHelper.KEY_HAS_IMAGE]?.toBoolean() ?: false

                    notificationHelper.showChatMessageNotification(
                        conversationId = conversationId,
                        messageId = messageId,
                        senderName = senderName,
                        senderAvatarUrl = senderAvatarUrl,
                        text = text,
                        hasImage = hasImage,
                    )
                    ChatSyncWorker.enqueue(this@SnapletFirebaseMessagingService, conversationId)
                }

                PushNotificationType.NEW_MESSAGE_REACTION -> {
                    val conversationId =
                        data[NotificationHelper.KEY_CONVERSATION_ID] ?: return@launch
                    val messageId = data[NotificationHelper.KEY_MESSAGE_ID] ?: return@launch
                    val reactorName = data[NotificationHelper.KEY_REACTOR_NAME] ?: return@launch
                    val reactorAvatarUrl = data[NotificationHelper.KEY_REACTOR_AVATAR_URL]
                    val emoji = data[NotificationHelper.KEY_EMOJI] ?: return@launch

                    notificationHelper.showMessageReactionNotification(
                        conversationId = conversationId,
                        messageId = messageId,
                        reactorName = reactorName,
                        reactorAvatarUrl = reactorAvatarUrl,
                        emoji = emoji,
                    )
                    ChatSyncWorker.enqueue(this@SnapletFirebaseMessagingService, conversationId)
                }

                else -> {
                    Logger.d("FCM type ignored in service: %s", pushType.name)
                }
            }
        }
    }
}
