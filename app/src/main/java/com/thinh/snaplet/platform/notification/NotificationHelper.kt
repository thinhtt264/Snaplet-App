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
        createNotificationChannel()
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        NotificationManagerCompat.from(context).notify(postId.hashCode(), notification)
    }

    /** Dismisses every notification posted by this app (e.g. when user opens the app). */
    fun cancelAllNotifications() {
        NotificationManagerCompat.from(context).cancelAll()
    }

    private fun createNotificationChannel() {
        val channel = android.app.NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            android.app.NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Notifications when someone reacts to your post"
        }
        val manager = context.getSystemService(android.app.NotificationManager::class.java)
        manager.createNotificationChannel(channel)
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
    }
}
