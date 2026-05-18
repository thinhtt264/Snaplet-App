package com.thinh.snaplet.platform.deeplink

import android.net.Uri

object DeepLinkUtils {
    fun buildSpotlightDeepLink(postId: String): Uri {
        return Uri.Builder()
            .scheme("snaplet")
            .authority("app")
            .appendPath("spotlight")
            .appendPath(postId)
            .build()
    }

    /** Push / FCM: `snaplet://chat/{conversationId}` */
    fun buildChatDeepLink(conversationId: String): Uri {
        return Uri.Builder()
            .scheme("snaplet")
            .authority("chat")
            .appendPath(conversationId)
            .build()
    }
}
