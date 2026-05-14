package com.thinh.snaplet.platform.notification

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.thinh.snaplet.MainActivity
import com.thinh.snaplet.R
import com.thinh.snaplet.platform.deeplink.DeepLinkUtils
import com.thinh.snaplet.utils.Logger
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    init {
        createNotificationChannels()
    }

    suspend fun showReactionNotification(
        title: String,
        body: String,
        postId: String,
        actorAvatarUrl: String?,
    ) {
        val deepLinkUri = DeepLinkUtils.buildSpotlightDeepLink(postId)
        val tapIntent =
            Intent(Intent.ACTION_VIEW, deepLinkUri, context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_POST_ID, postId)
                putExtra(EXTRA_DEEP_LINK_URI, deepLinkUri.toString())
                putExtra(EXTRA_NOTIFICATION_TYPE, TYPE_POST_REACTION)
            }

        val pendingIntent = PendingIntent.getActivity(
            context,
            postId.hashCode(),
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        val largeIconBitmap = loadAvatarBitmap(actorAvatarUrl) ?: loadLogoBitmap()
        largeIconBitmap?.let(notificationBuilder::setLargeIcon)

        val notification = notificationBuilder.build()

        if (!canPostNotifications()) return

        NotificationManagerCompat.from(context).notify(postId.hashCode(), notification)
    }

    suspend fun showChatMessageNotification(
        conversationId: String,
        @Suppress("UNUSED_PARAMETER") messageId: String,
        senderName: String,
        senderAvatarUrl: String?,
        text: String?,
        hasImage: Boolean,
    ) {
        if (!canPostNotifications()) return

        val deepLinkUri = DeepLinkUtils.buildChatDeepLink(conversationId)
        val tapIntent = buildChatMainActivityIntent(deepLinkUri, TYPE_CHAT_MESSAGE)
        val expandIntent = buildChatMainActivityIntent(deepLinkUri, TYPE_CHAT_MESSAGE)

        val tapPendingIntent = PendingIntent.getActivity(
            context,
            conversationId.hashCode(),
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val expandPendingIntent = PendingIntent.getActivity(
            context,
            conversationId.hashCode() + OPEN_CHAT_ACTION_REQUEST_OFFSET,
            expandIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val body = when {
            hasImage && text.isNullOrBlank() ->
                context.getString(R.string.chat_notification_photo_only)
            hasImage ->
                context.getString(R.string.chat_notification_photo_with_caption, text)
            else -> text.orEmpty()
        }

        val notificationBuilder = NotificationCompat.Builder(context, CHAT_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle(senderName)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(tapPendingIntent)
            .addAction(
                0,
                context.getString(R.string.notification_action_open_chat),
                expandPendingIntent,
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setGroup(CHAT_GROUP_PREFIX + conversationId)

        val largeIconBitmap = loadAvatarBitmap(senderAvatarUrl) ?: loadLogoBitmap()
        largeIconBitmap?.let(notificationBuilder::setLargeIcon)

        NotificationManagerCompat.from(context)
            .notify(conversationId.hashCode(), notificationBuilder.build())
    }

    suspend fun showMessageReactionNotification(
        conversationId: String,
        messageId: String,
        reactorName: String,
        reactorAvatarUrl: String?,
        emoji: String,
    ) {
        if (!canPostNotifications()) return

        val deepLinkUri = DeepLinkUtils.buildChatDeepLink(conversationId)
        val tapIntent = buildChatMainActivityIntent(deepLinkUri, TYPE_MESSAGE_REACTION)
        val pendingIntent = PendingIntent.getActivity(
            context,
            (conversationId + messageId).hashCode(),
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notificationBuilder = NotificationCompat.Builder(context, CHAT_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle(reactorName)
            .setContentText(
                context.getString(R.string.chat_notification_message_reaction, emoji),
            )
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setGroup(CHAT_GROUP_PREFIX + conversationId)

        val largeIconBitmap = loadAvatarBitmap(reactorAvatarUrl) ?: loadLogoBitmap()
        largeIconBitmap?.let(notificationBuilder::setLargeIcon)

        NotificationManagerCompat.from(context).notify(
            (conversationId + messageId).hashCode(),
            notificationBuilder.build(),
        )
    }

    /** Dismisses every notification posted by this app (e.g. when user opens the app). */
    fun cancelAllNotifications() {
        NotificationManagerCompat.from(context).cancelAll()
    }

    private fun buildChatMainActivityIntent(
        deepLinkUri: android.net.Uri,
        notificationType: String,
    ): Intent =
        Intent(Intent.ACTION_VIEW, deepLinkUri, context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_DEEP_LINK_URI, deepLinkUri.toString())
            putExtra(EXTRA_NOTIFICATION_TYPE, notificationType)
        }

    private fun canPostNotifications(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }
        return true
    }

    private fun createNotificationChannels() {
        val manager = context.getSystemService(android.app.NotificationManager::class.java)
        val reactions = android.app.NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            android.app.NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Notifications when someone reacts to your post"
        }
        val chat = android.app.NotificationChannel(
            CHAT_CHANNEL_ID,
            CHAT_CHANNEL_NAME,
            android.app.NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.chat_notification_channel_description)
            enableVibration(true)
        }
        manager.createNotificationChannel(reactions)
        manager.createNotificationChannel(chat)
    }

    private suspend fun loadAvatarBitmap(url: String?): Bitmap? {
        if (url.isNullOrBlank()) return null

        val request = ImageRequest.Builder(context)
            .data(url)
            .allowHardware(false)
            .build()

        return runCatching {
            val result = context.imageLoader.execute(request)
            (result as? SuccessResult)?.drawable?.toBitmap()
        }.onFailure { throwable ->
            Logger.w(throwable, "Failed to load notification avatar from %s", url)
        }.getOrNull()
    }

    private fun loadLogoBitmap(): Bitmap? {
        return runCatching {
            ContextCompat.getDrawable(context, R.mipmap.ic_launcher_round)?.toBitmap()
        }.onFailure { throwable ->
            Logger.w(throwable, "Failed to load logo bitmap for notification")
        }.getOrNull()
    }

    companion object {
        const val KEY_TYPE = "type"
        const val KEY_POST_ID = "postId"
        const val KEY_TITLE = "title"
        const val KEY_BODY = "body"
        const val KEY_ACTOR_AVATAR_URL = "actorAvatarUrl"

        const val CHANNEL_ID = "reactions_channel"
        private const val CHANNEL_NAME = "Reactions"
        const val EXTRA_POST_ID = "postId"
        const val EXTRA_DEEP_LINK_URI = "deepLinkUri"
        const val EXTRA_NOTIFICATION_TYPE = "notificationType"
        const val TYPE_POST_REACTION = "post_reaction"
        const val TYPE_CHAT_MESSAGE = "chat_message"
        const val TYPE_MESSAGE_REACTION = "message_reaction"

        const val KEY_CONVERSATION_ID = "conversationId"
        const val KEY_MESSAGE_ID = "messageId"
        const val KEY_SENDER_NAME = "senderName"
        const val KEY_SENDER_AVATAR_URL = "senderAvatarUrl"
        const val KEY_TEXT = "text"
        const val KEY_HAS_IMAGE = "hasImage"
        const val KEY_REACTOR_NAME = "reactorName"
        const val KEY_REACTOR_AVATAR_URL = "reactorAvatarUrl"
        const val KEY_EMOJI = "emoji"

        private const val CHAT_CHANNEL_ID = "chat_messages"
        private const val CHAT_CHANNEL_NAME = "Messages"
        private const val CHAT_GROUP_PREFIX = "chat_group_"
        private const val OPEN_CHAT_ACTION_REQUEST_OFFSET = 31
    }
}
