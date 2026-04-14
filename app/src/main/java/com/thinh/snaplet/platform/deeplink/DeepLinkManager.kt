package com.thinh.snaplet.platform.deeplink

import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import com.thinh.snaplet.platform.notification.NotificationHelper
import com.thinh.snaplet.utils.Logger
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeepLinkManager @Inject constructor() {
    companion object {
        private const val APP_SCHEME = "snaplet"
        private const val APP_HOST = "app"
        private const val WEB_SCHEME = "https"
        private const val WEB_HOST = "snaplet-cam.netlify.app"
        private const val SPOTLIGHT_PATH = "spotlight"
    }

    private val _events = MutableSharedFlow<DeepLinkEvent>(
        replay = 1,              // Replay last event to new subscribers (fix cold start)
        extraBufferCapacity = 1  // Buffer 1 additional event if processing is slow
    )

    val events: SharedFlow<DeepLinkEvent> = _events.asSharedFlow()

    suspend fun handleIntent(intent: Intent?) {
        if (intent == null) return
        if (intent.action == Intent.ACTION_VIEW && handleUri(intent.data)) return

        val deepLinkString = intent.getStringExtra(NotificationHelper.EXTRA_DEEP_LINK_URI)
            ?: intent.extras?.getString(NotificationHelper.EXTRA_DEEP_LINK_URI)
        if (!deepLinkString.isNullOrBlank() && handleUri(deepLinkString.toUri())) return
    }

    private suspend fun handleUri(uri: Uri?): Boolean {
        if (uri == null) return false
        val isAppScheme = uri.scheme == APP_SCHEME && uri.host == APP_HOST
        val isWebScheme = uri.scheme == WEB_SCHEME && uri.host == WEB_HOST
        if (!isAppScheme && !isWebScheme) return false

        Logger.d("🔗 DeepLink received: $uri")

        val userName = uri.getQueryParameter("userName")
        if (!userName.isNullOrBlank()) {
            _events.emit(DeepLinkEvent.FriendRequest(userName))
            return true
        }

        val postIdFromQuery = uri.getQueryParameter("postId")
        if (!postIdFromQuery.isNullOrBlank()) {
            _events.emit(DeepLinkEvent.OpenSpotlightPost(postIdFromQuery))
            return true
        }

        val path = uri.pathSegments
        if (path.size >= 2 && path[0] == SPOTLIGHT_PATH && path[1].isNotBlank()) {
            _events.emit(DeepLinkEvent.OpenSpotlightPost(path[1]))
            return true
        }
        return false
    }
}