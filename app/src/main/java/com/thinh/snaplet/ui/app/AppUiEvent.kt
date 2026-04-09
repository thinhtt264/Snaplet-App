package com.thinh.snaplet.ui.app

sealed interface AppUiEvent {
    object NavigateToAuthGraph : AppUiEvent

    object NavigateToHomeGraph : AppUiEvent

    data class NavigateToSpotlightPost(val postId: String) : AppUiEvent
}
