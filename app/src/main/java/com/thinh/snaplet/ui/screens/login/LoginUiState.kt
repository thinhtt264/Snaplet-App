package com.thinh.snaplet.ui.screens.login

enum class LoginStep {
    EMAIL,
    PASSWORD
}

data class LoginUiState(
    val currentStep: LoginStep = LoginStep.EMAIL,
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val isPasswordVisible: Boolean = false
)

