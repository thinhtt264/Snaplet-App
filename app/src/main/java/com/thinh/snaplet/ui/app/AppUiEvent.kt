package com.thinh.snaplet.ui.app

sealed interface AppUiEvent {
    object NavigateToAuthGraph : AppUiEvent

    object NavigateToHomeGraph : AppUiEvent

    data class NavigateToSpotlightPost(val postId: String) : AppUiEvent

    data class NavigateToChat(
        val conversationId: String,
        val partnerName: String = "",
        val partnerAvatarUrl: String? = null,
    ) : AppUiEvent
}
