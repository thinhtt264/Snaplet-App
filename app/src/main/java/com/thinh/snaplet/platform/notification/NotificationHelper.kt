package com.thinh.snaplet.platform.notification

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.thinh.snaplet.MainActivity
import com.thinh.snaplet.R
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

    fun showReactionNotification(
        title: String,
        body: String,
        postId: String,
    ) {
        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_POST_ID, postId)
            putExtra(EXTRA_NOTIFICATION_TYPE, TYPE_POST_REACTION)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            postId.hashCode(),
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

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

    companion object {
        const val CHANNEL_ID = "reactions_channel"
        private const val CHANNEL_NAME = "Reactions"
        const val EXTRA_POST_ID = "postId"
        const val EXTRA_NOTIFICATION_TYPE = "notificationType"
        const val TYPE_POST_REACTION = "post_reaction"
    }
}
