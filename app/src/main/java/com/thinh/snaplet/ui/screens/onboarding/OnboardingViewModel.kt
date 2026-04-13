package com.thinh.snaplet.ui.screens.onboarding

import android.content.Context
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thinh.snaplet.data.repository.auth.AuthRepository
import com.thinh.snaplet.platform.GoogleSignInManager
import com.thinh.snaplet.utils.Logger
import com.thinh.snaplet.utils.network.onFailure
import com.thinh.snaplet.utils.network.onSuccess
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OnboardingUiState(
    val isLoading: Boolean = false,
)

sealed interface OnboardingUiEvent {
    data class NavigateToRegister(val firstName: String?, val lastName: String?) : OnboardingUiEvent
}

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val googleSignInManager: GoogleSignInManager,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<OnboardingUiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    fun loginWithGoogle(context: Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val idToken = googleSignInManager.signIn(context)

                authRepository.loginWithGoogle(idToken)
                    .onSuccess { response ->
                        if (response.requiresOnboarding) {
                            _uiEvent.emit(
                                OnboardingUiEvent.NavigateToRegister(
                                    firstName = response.user.firstName,
                                    lastName = response.user.lastName
                                )
                            )
                        }
                    }
                    .onFailure { error ->
                        Logger.e("LoginWithGoogle failed: ${error.message}")
                    }
            } catch (e: GetCredentialException) {
                Logger.e("GoogleSignIn failed: ${e.message}")
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
}