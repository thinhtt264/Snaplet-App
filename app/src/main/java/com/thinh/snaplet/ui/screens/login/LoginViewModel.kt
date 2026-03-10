package com.thinh.snaplet.ui.screens.login

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thinh.snaplet.R
import com.thinh.snaplet.data.model.user.UserProfile
import com.thinh.snaplet.data.repository.UserRepository
import com.thinh.snaplet.data.repository.auth.AuthRepository
import com.thinh.snaplet.ui.common.UiText
import com.thinh.snaplet.utils.Logger
import com.thinh.snaplet.utils.ValidationConstants
import com.thinh.snaplet.utils.network.ApiError
import com.thinh.snaplet.utils.network.ApiErrorCode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
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

    private val _uiEvent = MutableSharedFlow<LoginUIEvent>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.SUSPEND
    )
    val uiEvent: SharedFlow<LoginUIEvent> = _uiEvent.asSharedFlow()

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

            _uiState.update {
                it.copy(
                    isLoading = false,
                    errorMessage = null,
                    currentStep = LoginStep.PASSWORD
                )
            }
        }
    }

    fun onLogin() {
        viewModelScope.launch {
            val currentState = _uiState.value

            val passwordError = validatePassword(currentState.password)

            if (passwordError != null) {
                _uiState.update { it.copy(passwordError = passwordError) }
                return@launch
            }

            _uiState.update { it.copy(isLoading = true) }

            val result = authRepository.login(
                email = currentState.email,
                password = currentState.password
            )

            result.fold(
                onSuccess = { userProfile ->
                    _uiState.update { it.copy(isLoading = false) }
                    _uiEvent.emit(LoginUIEvent.LoginSuccess)
                },
                onFailure = { error ->
                    _uiState.update { it.copy(isLoading = false) }
                    handleLoginError(error)
                }
            )
        }
    }

    fun onNavigateToRegister() {
        viewModelScope.launch {
            _uiEvent.emit(LoginUIEvent.NavigateToRegister)
        }
    }

    private fun validateEmail(email: String): UiText? {
        return when {
            email.isBlank() -> UiText.StringResource(R.string.email_required)
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() ->
                UiText.StringResource(R.string.email_invalid)

            else -> null
        }
    }

    private fun validatePassword(password: String): UiText? {
        return when {
            password.length < ValidationConstants.PASSWORD_MIN_LENGTH -> UiText.StringResource(R.string.password_requirement)
            else -> null
        }
    }

    private suspend fun handleLoginError(error: ApiError) {
        val message =
            if (error.errorCode == ApiErrorCode.INVALID_CREDENTIALS) {
                UiText.StringResource(R.string.invalid_credentials)
            } else {
                UiText.DynamicString(error.message)
            }
        _uiEvent.emit(LoginUIEvent.ShowErrorPopup(message))
    }
}