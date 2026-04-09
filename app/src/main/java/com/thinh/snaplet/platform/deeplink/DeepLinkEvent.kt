package com.thinh.snaplet.platform.deeplink

sealed class DeepLinkEvent {
    /**
     * Friend request deeplink event
     * Triggered when user taps friend request deeplink
     *
     * @param userName Username to show friend request for
     */
    data class FriendRequest(val userName: String) : DeepLinkEvent()

    data class OpenSpotlightPost(val postId: String) : DeepLinkEvent()
}

