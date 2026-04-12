package com.thinh.snaplet.platform.deeplink

sealed class DeepLinkEvent {
    
    data class FriendRequest(val userName: String) : DeepLinkEvent()

    data class OpenSpotlightPost(val postId: String) : DeepLinkEvent()
}

