package com.thinh.snaplet.ui.screens.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    // Inject your repositories here if needed
    // private val authRepository: AuthRepository,
    // private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        checkAppState()
    }

    private fun checkAppState() {
        viewModelScope.launch {
            // Examples:
            // - Check authentication token
            // - Load user preferences
            // - Initialize app configurations
            // - Check for app updates
            // - Preload cached data
            _isLoading.value = false
        }
    }
}

