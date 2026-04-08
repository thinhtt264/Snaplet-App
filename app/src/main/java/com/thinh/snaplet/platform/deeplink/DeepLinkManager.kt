package com.thinh.snaplet.platform.deeplink

import android.content.Intent
import com.thinh.snaplet.platform.notification.NotificationHelper
import com.thinh.snaplet.utils.Logger
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeepLinkManager @Inject constructor() {

    private val _events = MutableSharedFlow<DeepLinkEvent>(
        replay = 1,              // Replay last event to new subscribers (fix cold start)
        extraBufferCapacity = 1  // Buffer 1 additional event if processing is slow
    )

    val events: SharedFlow<DeepLinkEvent> = _events.asSharedFlow()

    suspend fun handleDeepLink(intent: Intent?) {
        if (intent?.action != Intent.ACTION_VIEW) return

        val data = intent.data ?: return

        Logger.d("🔗 DeepLink received: $data")

        val userName = data.getQueryParameter("userName")

        if (!userName.isNullOrBlank()) {
            _events.emit(DeepLinkEvent.FriendRequest(userName))
        }
    }

    suspend fun handleNotificationIntent(intent: Intent?) {
        val postId = intent?.getStringExtra(NotificationHelper.EXTRA_POST_ID)
            ?: intent?.extras?.getString(NotificationHelper.EXTRA_POST_ID)
            ?: return

        _events.emit(DeepLinkEvent.OpenSpotlightPost(postId))
    }
}