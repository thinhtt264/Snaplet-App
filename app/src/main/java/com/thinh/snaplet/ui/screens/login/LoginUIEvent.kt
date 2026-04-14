package com.thinh.snaplet.ui.screens.login

sealed interface LoginUIEvent {

    data object LoginSuccess : LoginUIEvent

    data object NavigateToRegister : LoginUIEvent
}

