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
}
