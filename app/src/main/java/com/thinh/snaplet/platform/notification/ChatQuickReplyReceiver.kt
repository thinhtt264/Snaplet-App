package com.thinh.snaplet.platform.notification

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import com.thinh.snaplet.R
import com.thinh.snaplet.domain.chat.SendMessageUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import javax.inject.Inject

@AndroidEntryPoint
class ChatQuickReplyReceiver : BroadcastReceiver() {

    @Inject
    lateinit var sendMessageUseCase: SendMessageUseCase

    @Inject
    lateinit var notificationHelper: NotificationHelper

    override fun onReceive(context: Context, intent: Intent) {
        val conversationId =
            intent.getStringExtra(NotificationHelper.KEY_CONVERSATION_ID) ?: return
        val notificationId =
            intent.getIntExtra(NotificationHelper.KEY_NOTIFICATION_ID, -1)
        if (notificationId == -1) return

        val replyText = RemoteInput.getResultsFromIntent(intent)
            ?.getCharSequence(NotificationHelper.KEY_QUICK_REPLY_INPUT)
            ?.toString()
            ?.trim()
            .takeIf { !it.isNullOrBlank() } ?: return

        val pendingResult = goAsync()
        val nm = NotificationManagerCompat.from(context)

        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            nm.notify(
                notificationId,
                NotificationCompat.Builder(context, NotificationHelper.CHAT_CHANNEL_ID)
                    .setSmallIcon(R.mipmap.ic_launcher_round)
                    .setContentText(context.getString(R.string.notification_reply_sending))
                    .setProgress(0, 0, true)
                    .build(),
            )
        }

        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            try {
                withTimeout(QUICK_REPLY_SEND_TIMEOUT_MS) {
                    sendMessageUseCase(conversationId, replyText)
                }
                dismissQuickReplySending(context, nm, notificationId)
            } catch (_: TimeoutCancellationException) {
                dismissQuickReplySending(context, nm, notificationId)
            } catch (e: CancellationException) {
                dismissQuickReplySending(context, nm, notificationId)
                throw e
            } catch (_: Exception) {
                if (ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS,
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    nm.notify(
                        notificationId,
                        NotificationCompat.Builder(
                            context,
                            NotificationHelper.CHAT_CHANNEL_ID,
                        )
                            .setSmallIcon(R.mipmap.ic_launcher_round)
                            .setContentText(
                                context.getString(R.string.notification_reply_failed),
                            )
                            .setContentIntent(
                                notificationHelper.openChatContentPendingIntent(
                                    conversationId,
                                ),
                            )
                            .setAutoCancel(true)
                            .build(),
                    )
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    /**
     * Some OEMs leave an indeterminate progress bar visible after [NotificationManagerCompat.cancel].
     * Post a one-frame update with progress disabled, then cancel.
     */
    private fun dismissQuickReplySending(
        context: Context,
        nm: NotificationManagerCompat,
        notificationId: Int,
    ) {
        try {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                nm.notify(
                    notificationId,
                    NotificationCompat.Builder(context, NotificationHelper.CHAT_CHANNEL_ID)
                        .setSmallIcon(R.mipmap.ic_launcher_round)
                        .setContentText(" ")
                        .setProgress(0, 0, false)
                        .setOngoing(false)
                        .setSilent(true)
                        .build(),
                )
            }
            nm.cancel(notificationId)
        } catch (_: Exception) {
            try {
                nm.cancel(notificationId)
            } catch (_: Exception) {
            }
        }
    }

    private companion object {
        private const val QUICK_REPLY_SEND_TIMEOUT_MS = 15_000L
    }
}
