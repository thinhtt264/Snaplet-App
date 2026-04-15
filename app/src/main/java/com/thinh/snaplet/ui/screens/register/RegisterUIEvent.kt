package com.thinh.snaplet.ui.screens.register

sealed interface RegisterUIEvent {

    data object NavigateToLogin : RegisterUIEvent

    data object NavigateToOnboarding : RegisterUIEvent
}