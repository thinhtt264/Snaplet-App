package com.thinh.snaplet.ui.screens.register

import android.util.Patterns
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.thinh.snaplet.R
import com.thinh.snaplet.data.repository.auth.AuthRepository
import com.thinh.snaplet.navigation.Register
import com.thinh.snaplet.ui.common.UiText
import com.thinh.snaplet.utils.ValidationConstants
import com.thinh.snaplet.utils.network.ApiErrorCode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.BufferOverflow
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
class RegisterViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    init {
        val registerRoute = savedStateHandle.toRoute<Register>()
        val isFromGoogleLogin = registerRoute.isFromGoogleLogin
        _uiState.update {
            it.copy(
                isFromGoogleLogin = isFromGoogleLogin,
                firstName = registerRoute.firstName ?: "",
                lastName = registerRoute.lastName ?: "",
                currentStep = if (isFromGoogleLogin) {
                    RegisterStep.USERNAME
                } else {
                    RegisterStep.EMAIL
                }
            )
        }
    }

    private val _uiEvent = MutableSharedFlow<RegisterUIEvent>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.SUSPEND
    )
    val uiEvent: SharedFlow<RegisterUIEvent> = _uiEvent.asSharedFlow()

    fun onEmailChange(email: String) {
        _uiState.update { it.copy(email = email, emailError = null, errorMessage = null) }
    }

    fun onUsernameChange(username: String) {
        val maxLen = ValidationConstants.USERNAME_MAX_LENGTH
        val exceeded = username.length > maxLen
        val hasWhitespace = username.any { it.isWhitespace() }
        _uiState.update {
            it.copy(
                username = username,
                usernameError = when {
                    exceeded -> registerMaxLengthError(maxLen)
                    hasWhitespace -> UiText.StringResource(R.string.username_no_spaces)
                    else -> null
                },
                errorMessage = null
            )
        }
    }

    fun onFirstNameChange(firstName: String) {
        val maxLen = ValidationConstants.NAME_MAX_LENGTH
        val exceeded = firstName.length > maxLen
        _uiState.update {
            it.copy(
                firstName = firstName,
                firstNameError = if (exceeded) {
                    registerMaxLengthError(ValidationConstants.NAME_MAX_LENGTH)
                } else {
                    null
                },
                errorMessage = null
            )
        }
    }

    fun onLastNameChange(lastName: String) {
        val maxLen = ValidationConstants.NAME_MAX_LENGTH
        val exceeded = lastName.length > maxLen
        _uiState.update {
            it.copy(
                lastName = lastName,
                lastNameError = if (exceeded) {
                    registerMaxLengthError(ValidationConstants.NAME_MAX_LENGTH)
                } else {
                    null
                },
                errorMessage = null
            )
        }
    }

    fun onPasswordChange(password: String) {
        val passwordError = validatePassword(password)

        _uiState.update {
            it.copy(
                password = password,
                passwordError = passwordError,
                errorMessage = null
            )
        }
    }

    fun onPasswordVisibilityToggle() {
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun onScrollToPage(step: RegisterStep) {
        val currentState = _uiState.value
        if (currentState.currentStep != step && !currentState.isLoading) {
            when (step) {
                RegisterStep.EMAIL -> {
                    _uiState.update {
                        it.copy(
                            currentStep = RegisterStep.EMAIL,
                            username = "",
                            usernameError = null,
                            firstName = "",
                            lastName = "",
                            firstNameError = null,
                            lastNameError = null,
                            password = "",
                            passwordError = null,
                            errorMessage = null
                        )
                    }
                }

                RegisterStep.USERNAME -> {
                    _uiState.update {
                        it.copy(
                            currentStep = RegisterStep.USERNAME,
                            password = "",
                            passwordError = null,
                            errorMessage = null
                        )
                    }
                }

                RegisterStep.PASSWORD -> {
                    _uiState.update {
                        it.copy(
                            currentStep = RegisterStep.PASSWORD,
                            errorMessage = null
                        )
                    }
                }
            }
        }
    }

    fun onContinueFromEmail() {
        viewModelScope.launch {
            val currentState = _uiState.value

            if (currentState.currentStep != RegisterStep.EMAIL || currentState.isLoading) {
                return@launch
            }

            val emailError = validateEmail(currentState.email)
            if (emailError != null) {
                _uiState.update { it.copy(emailError = emailError) }
                return@launch
            }

            _uiState.update { it.copy(isLoading = true, emailError = null, errorMessage = null) }

            val result = authRepository.checkEmailAvailability(currentState.email)

            result.fold(
                onSuccess = { isAvailable ->
                    if (!isAvailable) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                emailError = UiText.StringResource(R.string.email_already_taken)
                            )
                        }
                        return@launch
                    }

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            currentStep = RegisterStep.USERNAME,
                            isFromGoogleLogin = false
                        )
                    }
                },
                onFailure = { error ->
                    if (error.errorCode == ApiErrorCode.EMAIL_REGISTERED_WITH_GOOGLE) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                showGoogleRegisteredDialog = true
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                emailError = UiText.DynamicString(error.message)
                            )
                        }
                    }
                }
            )
        }
    }

    fun onContinueFromUsername() {
        viewModelScope.launch {
            val currentState = _uiState.value

            if (currentState.currentStep != RegisterStep.USERNAME || currentState.isLoading) {
                return@launch
            }

            val usernameError = validateUsername(currentState.username)
            val firstNameError = validateFirstName(currentState.firstName)
            val lastNameError = validateLastName(currentState.lastName)

            if (usernameError != null || firstNameError != null || lastNameError != null) {
                _uiState.update {
                    it.copy(
                        usernameError = usernameError,
                        firstNameError = firstNameError,
                        lastNameError = lastNameError
                    )
                }
                return@launch
            }

            _uiState.update {
                it.copy(
                    isLoading = true,
                    usernameError = null,
                    firstNameError = null,
                    lastNameError = null,
                    errorMessage = null
                )
            }
            val result = authRepository.checkUsernameAvailability(currentState.username)

            result.fold(
                onSuccess = { isAvailable ->
                    if (!isAvailable) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                usernameError = UiText.StringResource(R.string.username_already_taken)
                            )
                        }
                        return@launch
                    }

                    if (currentState.isFromGoogleLogin) {
                        onCompleteOnboarding()
                    } else {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                currentStep = RegisterStep.PASSWORD
                            )
                        }
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            usernameError = UiText.DynamicString(error.message)
                        )
                    }
                }
            )
        }
    }

    private fun onCompleteOnboarding() {
        viewModelScope.launch {
            val currentState = _uiState.value

            val result = authRepository.completeOnboarding(
                username = currentState.username,
                firstName = currentState.firstName,
                lastName = currentState.lastName
            )

            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            showPostRegisterWidgetPromo = true,
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = UiText.DynamicString(error.message))
                    }
                }
            )
        }
    }

    fun onRegister() {
        viewModelScope.launch {
            val currentState = _uiState.value

            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val result = authRepository.register(
                email = currentState.email,
                username = currentState.username,
                firstName = currentState.firstName,
                lastName = currentState.lastName,
                password = currentState.password
            )

            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            showPostRegisterWidgetPromo = true,
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = UiText.DynamicString(error.message))
                    }
                }
            )
        }
    }

    fun onErrorDismissed() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun onGoogleLoginDialogDismiss() {
        _uiState.update { it.copy(showGoogleRegisteredDialog = false) }
    }

    fun onGoogleLoginDialogConfirm() {
        viewModelScope.launch {
            _uiState.update { it.copy(showGoogleRegisteredDialog = false) }
            _uiEvent.emit(RegisterUIEvent.NavigateToOnboarding)
        }
    }

    fun onNavigateToLogin() {
        viewModelScope.launch {
            _uiEvent.emit(RegisterUIEvent.NavigateToLogin)
        }
    }

    suspend fun finishRegistrationPromo() {
        authRepository.activatePendingRegistrationSession()
        _uiState.update { it.copy(showPostRegisterWidgetPromo = false) }
    }

    private fun validateEmail(email: String): UiText? {
        return when {
            email.isBlank() -> UiText.StringResource(R.string.email_required)
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() ->
                UiText.StringResource(R.string.email_invalid)

            else -> null
        }
    }

    private fun validateUsername(username: String): UiText? {
        return when {
            username.isBlank() -> UiText.StringResource(R.string.username_required)
            username.length < ValidationConstants.USERNAME_MIN_LENGTH -> UiText.StringResource(R.string.username_requirement1)
            username.length > ValidationConstants.USERNAME_MAX_LENGTH ->
                registerMaxLengthError(ValidationConstants.USERNAME_MAX_LENGTH)

            username.any { it.isWhitespace() } ->
                UiText.StringResource(R.string.username_no_spaces)

            !username.matches(ValidationConstants.USERNAME_PATTERN) -> UiText.StringResource(R.string.username_invalid_characters)
            else -> null
        }
    }

    private fun validatePassword(password: String): UiText? {
        return when {
            password.length < ValidationConstants.PASSWORD_MIN_LENGTH -> UiText.StringResource(R.string.password_requirement)
            else -> null
        }
    }

    private fun validateFirstName(firstName: String): UiText? {
        return when {
            firstName.isBlank() -> UiText.StringResource(R.string.first_name_required)
            firstName.length > ValidationConstants.NAME_MAX_LENGTH ->
                registerMaxLengthError(ValidationConstants.NAME_MAX_LENGTH)

            else -> null
        }
    }

    private fun validateLastName(lastName: String): UiText? {
        return when {
            lastName.isBlank() -> UiText.StringResource(R.string.last_name_required)
            lastName.length > ValidationConstants.NAME_MAX_LENGTH ->
                registerMaxLengthError(ValidationConstants.NAME_MAX_LENGTH)

            else -> null
        }
    }

    private fun registerMaxLengthError(maxChars: Int): UiText =
        UiText.StringResource(R.string.register_name_max_length, listOf(maxChars))
}