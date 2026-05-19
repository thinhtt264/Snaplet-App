package com.thinh.snaplet.platform.notification

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Shader
import android.os.Build
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.app.RemoteInput
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.IconCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.scale
import androidx.core.net.toUri
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.thinh.snaplet.MainActivity
import com.thinh.snaplet.R
import com.thinh.snaplet.platform.deeplink.DeepLinkUtils
import com.thinh.snaplet.utils.Logger
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.ArrayDeque
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private data class ChatNotifEntry(
        val messageId: String,
        val senderName: String,
        val body: String,
        val timestamp: Long,
    )

    private val chatNotifMessagesByConversation =
        ConcurrentHashMap<String, ArrayDeque<ChatNotifEntry>>()

    init {
        createNotificationChannels()
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    suspend fun showCustomNotification(
        title: String,
        body: String,
        deeplink: String,
        largeIconUrl: String?,
    ) {
        val deepLinkUri = deeplink.toUri()
        val notificationId = deeplink.hashCode()
        val tapIntent =
            Intent(Intent.ACTION_VIEW, deepLinkUri, context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_DEEP_LINK_URI, deeplink)
                putExtra(EXTRA_NOTIFICATION_TYPE, TYPE_CUSTOM)
            }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
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

        loadAvatarBitmap(largeIconUrl)?.let { largeIcon ->
            notificationBuilder
                .setSmallIcon(IconCompat.createWithBitmap(largeIcon))
                .setLargeIcon(largeIcon)
        }

        val notification = notificationBuilder.build()

        if (!canPostNotifications()) return

        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    suspend fun showChatMessageNotification(
        conversationId: String,
        messageId: String,
        senderName: String,
        senderAvatarUrl: String?,
        text: String?,
        hasImage: Boolean,
    ) {
        if (!canPostNotifications()) return

        val body = when {
            hasImage && text.isNullOrBlank() ->
                context.getString(R.string.chat_notification_photo_only)

            hasImage ->
                context.getString(R.string.chat_notification_photo_with_caption, text)

            else -> text.orEmpty()
        }

        val merged = appendChatNotifMessage(
            conversationId = conversationId,
            messageId = messageId,
            senderName = senderName,
            body = body,
        )
        val latest = merged.last()

        // Load avatar first — needed for both ShortcutInfo and MessagingStyle person icon
        val avatarIcon = loadAvatarBitmap(senderAvatarUrl)
            ?.let { IconCompat.createWithBitmap(toCircularBitmap(it)) }
            ?: IconCompat.createWithBitmap(createInitialBitmap(senderName))

        // Push conversation shortcut — Android uses its icon as the notification icon (like Messenger)
        val senderPerson = Person.Builder()
            .setName(senderName)
            .setIcon(avatarIcon)
            .setImportant(true)
            .build()
        val shortcutId = CONV_SHORTCUT_PREFIX + conversationId
        val shortcut = ShortcutInfoCompat.Builder(context, shortcutId)
            .setLongLived(true)
            .setIntent(Intent(context, MainActivity::class.java).apply {
                action = Intent.ACTION_DEFAULT
            })
            .setShortLabel(senderName)
            .setIcon(avatarIcon)
            .setPerson(senderPerson)
            .build()
        ShortcutManagerCompat.pushDynamicShortcut(context, shortcut)

        val deepLinkUri = DeepLinkUtils.buildChatDeepLink(conversationId)
        val tapIntent = buildChatMainActivityIntent(
            deepLinkUri = deepLinkUri,
            notificationType = TYPE_CHAT_MESSAGE,
            partnerName = latest.senderName,
            partnerAvatarUrl = senderAvatarUrl,
        )
        val expandIntent = buildChatMainActivityIntent(
            deepLinkUri = deepLinkUri,
            notificationType = TYPE_CHAT_MESSAGE,
            partnerName = latest.senderName,
            partnerAvatarUrl = senderAvatarUrl,
        )

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

        val remoteInput = RemoteInput.Builder(KEY_QUICK_REPLY_INPUT)
            .setLabel(context.getString(R.string.notification_reply_hint))
            .build()

        val replyPendingIntent = PendingIntent.getBroadcast(
            context,
            conversationId.hashCode() + QUICK_REPLY_REQUEST_OFFSET,
            Intent(context, ChatQuickReplyReceiver::class.java).apply {
                putExtra(KEY_CONVERSATION_ID, conversationId)
                putExtra(KEY_NOTIFICATION_ID, conversationId.hashCode())
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
        )

        val replyAction = NotificationCompat.Action.Builder(
            0,
            context.getString(R.string.notification_action_reply),
            replyPendingIntent,
        ).addRemoteInput(remoteInput).build()

        val selfPerson =
            Person.Builder().setName(context.getString(R.string.notification_me)).build()
        val messagingStyle = NotificationCompat.MessagingStyle(selfPerson)
        for (entry in merged) {
            val person = Person.Builder().setName(entry.senderName).setIcon(avatarIcon).build()
            messagingStyle.addMessage(entry.body, entry.timestamp, person)
        }

        val notificationBuilder = NotificationCompat.Builder(context, CHAT_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setShortcutId(shortcutId)
            .setContentTitle(latest.senderName)
            .setContentText(latest.body)
            .setStyle(messagingStyle)
            .setAutoCancel(true)
            .setContentIntent(tapPendingIntent)
            .addAction(replyAction)
            .addAction(
                0,
                context.getString(R.string.notification_action_open_chat),
                expandPendingIntent,
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setGroup(CHAT_GROUP_PREFIX + conversationId)

        NotificationManagerCompat.from(context)
            .notify(conversationId.hashCode(), notificationBuilder.build())
    }

    private fun appendChatNotifMessage(
        conversationId: String,
        messageId: String,
        senderName: String,
        body: String,
    ): List<ChatNotifEntry> {
        val deque = chatNotifMessagesByConversation.computeIfAbsent(conversationId) {
            ArrayDeque()
        }
        synchronized(deque) {
            deque.removeAll { it.messageId == messageId }
            deque.addLast(
                ChatNotifEntry(
                    messageId = messageId,
                    senderName = senderName,
                    body = body,
                    timestamp = System.currentTimeMillis(),
                ),
            )
            while (deque.size > CHAT_NOTIF_MAX_MERGED_MESSAGES) {
                deque.removeFirst()
            }
            return deque.toList()
        }
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    suspend fun showMessageReactionNotification(
        conversationId: String,
        messageId: String,
        reactorName: String,
        reactorAvatarUrl: String?,
        emoji: String,
    ) {
        showChatMessageNotification(
            conversationId = conversationId,
            messageId = messageId,
            senderName = reactorName,
            senderAvatarUrl = reactorAvatarUrl,
            text = context.getString(R.string.chat_notification_message_reaction, emoji),
            hasImage = false,
        )
    }

    /** Dismisses every notification posted by this app (e.g. when user opens the app). */
    fun cancelAllNotifications() {
        chatNotifMessagesByConversation.clear()
        NotificationManagerCompat.from(context).cancelAll()
    }

    private fun buildChatMainActivityIntent(
        deepLinkUri: android.net.Uri,
        notificationType: String,
        partnerName: String? = null,
        partnerAvatarUrl: String? = null,
    ): Intent =
        Intent(Intent.ACTION_VIEW, deepLinkUri, context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_DEEP_LINK_URI, deepLinkUri.toString())
            putExtra(EXTRA_NOTIFICATION_TYPE, notificationType)
            if (!partnerName.isNullOrBlank()) {
                putExtra(EXTRA_CHAT_PARTNER_NAME, partnerName)
            }
            if (!partnerAvatarUrl.isNullOrBlank()) {
                putExtra(EXTRA_CHAT_PARTNER_AVATAR_URL, partnerAvatarUrl)
            }
        }

    fun openChatContentPendingIntent(conversationId: String): PendingIntent {
        val deepLinkUri = DeepLinkUtils.buildChatDeepLink(conversationId)
        val tapIntent = buildChatMainActivityIntent(deepLinkUri, TYPE_CHAT_MESSAGE)
        return PendingIntent.getActivity(
            context,
            conversationId.hashCode(),
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun canPostNotifications(): Boolean {
        return !(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) != PackageManager.PERMISSION_GRANTED)
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

    private fun createInitialBitmap(name: String): Bitmap {
        val size = 128
        val output = createBitmap(size, size)
        val canvas = Canvas(output)
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.BLACK
        }
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, bgPaint)
        val initial = name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            textSize = size * 0.4f
            textAlign = Paint.Align.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        val textBounds = android.graphics.Rect()
        textPaint.getTextBounds(initial, 0, initial.length, textBounds)
        canvas.drawText(initial, size / 2f, size / 2f - (textBounds.top + textBounds.bottom) / 2f, textPaint)
        return output
    }

    private fun toCircularBitmap(src: Bitmap): Bitmap {
        val size = 128
        val output = createBitmap(size, size)
        val scaled = src.scale(size, size)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = BitmapShader(scaled, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        }
        Canvas(output).drawCircle(size / 2f, size / 2f, size / 2f, paint)
        return output
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

    companion object {
        const val KEY_TYPE = "type"
        const val KEY_TITLE = "title"
        const val KEY_BODY = "body"
        const val KEY_DEEPLINK = "deeplink"
        const val KEY_LARGE_ICON_URL = "largeIconUrl"

        const val CHANNEL_ID = "reactions_channel"
        private const val CHANNEL_NAME = "Reactions"
        const val EXTRA_DEEP_LINK_URI = "deepLinkUri"
        const val EXTRA_NOTIFICATION_TYPE = "notificationType"

        /** Shown in [com.thinh.snaplet.navigation.ChatConversation] header when opening chat from a notification tap. */
        const val EXTRA_CHAT_PARTNER_NAME = "chatPartnerName"
        const val EXTRA_CHAT_PARTNER_AVATAR_URL = "chatPartnerAvatarUrl"
        const val TYPE_CUSTOM = "custom"
        const val TYPE_CHAT_MESSAGE = "chat_message"
        const val KEY_CONVERSATION_ID = "conversationId"
        const val KEY_MESSAGE_ID = "messageId"
        const val KEY_SENDER_NAME = "senderName"
        const val KEY_SENDER_AVATAR_URL = "senderAvatarUrl"
        const val KEY_TEXT = "text"
        const val KEY_HAS_IMAGE = "hasImage"
        const val KEY_REACTOR_NAME = "reactorName"
        const val KEY_REACTOR_AVATAR_URL = "reactorAvatarUrl"
        const val KEY_EMOJI = "emoji"

        const val KEY_QUICK_REPLY_INPUT = "quick_reply_input"
        const val KEY_NOTIFICATION_ID = "notificationId"

        const val CHAT_CHANNEL_ID = "chat_messages"
        private const val CHAT_CHANNEL_NAME = "Messages"
        private const val CHAT_GROUP_PREFIX = "chat_group_"
        private const val CHAT_NOTIF_MAX_MERGED_MESSAGES = 3
        private const val OPEN_CHAT_ACTION_REQUEST_OFFSET = 31
        private const val QUICK_REPLY_REQUEST_OFFSET = 73
        private const val CONV_SHORTCUT_PREFIX = "conv_"
    }
}
