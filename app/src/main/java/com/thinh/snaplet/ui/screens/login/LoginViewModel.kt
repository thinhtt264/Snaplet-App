package com.thinh.snaplet.ui.screens.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thinh.snaplet.data.model.UserProfile
import com.thinh.snaplet.data.repository.auth.AuthRepository
import com.thinh.snaplet.data.repository.UserRepository
import com.thinh.snaplet.utils.Logger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    userRepository: UserRepository
) : ViewModel() {

    private val _currentUserProfile: Flow<UserProfile?> = userRepository.observeMyUserProfile()
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    init {
        observeUserProfile()
    }

    private fun observeUserProfile() {
        viewModelScope.launch {
            _currentUserProfile.collect { profile ->
                profile?.let { _uiState.update { it.copy(email = profile.email) } }
            }
        }
    }

    fun onEmailChange(email: String) {
        _uiState.update { it.copy(email = email, emailError = null, errorMessage = null) }
    }

    fun onPasswordChange(password: String) {
        _uiState.update { it.copy(password = password, passwordError = null, errorMessage = null) }
    }

    fun onPasswordVisibilityToggle() {
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun onBackToEmailStep() {
        _uiState.update {
            it.copy(
                currentStep = LoginStep.EMAIL,
                password = "",
                passwordError = null,
                errorMessage = null
            )
        }
    }

    fun onContinueFromEmail() {
        viewModelScope.launch {
            val currentState = _uiState.value

            val emailError = validateEmail(currentState.email)
            if (emailError != null) {
                _uiState.update { it.copy(emailError = emailError) }
                return@launch
            }

            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            try {
                // For now, we'll just validate the email format and proceed to password step
                // In the future, you can add an API call to check email availability here
                // Example: val result = authRepository.checkEmailExists(currentState.email)

                Logger.d("✅ Email validated: ${currentState.email}")
                _uiState.update {
                    it.copy(
                        isLoading = false, currentStep = LoginStep.PASSWORD
                    )
                }

            } catch (e: Exception) {
                Logger.e("❌ Email validation failed: ${e.message}")
                _uiState.update {
                    it.copy(
                        isLoading = false, emailError = e.message ?: "Unable to verify email"
                    )
                }
            }
        }
    }

    fun onLogin(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val currentState = _uiState.value

            // Validate password
            val passwordError = validatePassword(currentState.password)
            if (passwordError != null) {
                _uiState.update { it.copy(passwordError = passwordError) }
                return@launch
            }

            // Start loading
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            try {
                val result = authRepository.login(
                    email = currentState.email,
                    password = currentState.password
                )

                if (result.isSuccess) {
                    val userProfile: UserProfile = result.getOrThrow()
                    Logger.d("✅ Login successful for: ${userProfile.displayName}")

                    _uiState.update { it.copy(isLoading = false) }
                    onSuccess()
                } else {
                    val exception = result.exceptionOrNull()
                    Logger.e("❌ Login failed: ${exception?.message}")
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = exception?.message ?: "An error occurred during login"
                        )
                    }
                }

            } catch (e: Exception) {
                Logger.e("❌ Login exception: ${e.message}")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "An error occurred during login"
                    )
                }
            }
        }
    }

    private fun validateEmail(email: String): String? {
        return when {
            email.isBlank() -> "Email is required"
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> "Invalid email format"
            else -> null
        }
    }

    private fun validatePassword(password: String): String? {
        return when {
            password.isBlank() -> "Password is required"
            password.length < 8 -> "Password must be at least 8 characters"
            else -> null
        }
    }
}