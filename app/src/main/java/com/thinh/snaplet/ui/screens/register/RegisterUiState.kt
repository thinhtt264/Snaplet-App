package com.thinh.snaplet.ui.screens.register

import com.thinh.snaplet.ui.common.UiText

enum class RegisterStep {
    EMAIL,
    USERNAME,
    PASSWORD
}

data class RegisterUiState(
    val currentStep: RegisterStep = RegisterStep.EMAIL,
    val email: String = "",
    val username: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val errorMessage: UiText? = null,
    val emailError: UiText? = null,
    val usernameError: UiText? = null,
    val firstNameError: UiText? = null,
    val lastNameError: UiText? = null,
    val passwordError: UiText? = null,
    val isPasswordVisible: Boolean = false,
    val showPostRegisterWidgetPromo: Boolean = false,
    val isGoogleLogin: Boolean = false,
)