package com.thinh.snaplet.ui.screens.onboarding

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * OnboardingViewModel - ViewModel cho Onboarding screen
 * Có thể thêm logic business như:
 * - Check user session
 * - Save onboarding state
 * - Analytics tracking
 */
@HiltViewModel
class OnboardingViewModel @Inject constructor() : ViewModel() {

    /**
     * Handle create account action
     * Có thể thêm logic như validation, API call, etc.
     */
    fun onCreateAccount() {
        // TODO: Implement create account logic
        // For now, just navigate to home
    }

    /**
     * Handle login action
     * Có thể thêm logic như authentication, save session, etc.
     */
    fun onLogin() {
        // TODO: Implement login logic
        // For now, just navigate to home
    }
}