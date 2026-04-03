package com.thinh.snaplet.ui.screens.register

sealed interface RegisterUIEvent {

    data class ShowErrorPopup(val message: String) : RegisterUIEvent

    data object NavigateToLogin : RegisterUIEvent
}