package com.thinh.snaplet.ui.screens.friend_request

import com.thinh.snaplet.data.model.UserProfile

sealed class FriendRequestUiState {
    /**
     * Hidden state - No overlay shown
     */
    data object Hidden : FriendRequestUiState()

    /**
     * Loading state - Fetching user data
     * Shows loading indicator in overlay
     *
     * @param userName Username being loaded
     */
    data class Loading(val userName: String) : FriendRequestUiState()

    /**
     * Visible state - User data loaded successfully
     * Shows user profile in overlay
     *
     * @param userProfile User profile data
     * @param isLoading Whether a background action (e.g., send request) is in progress
     */
    data class Visible(
        val userProfile: UserProfile,
        val isLoading: Boolean = false
    ) : FriendRequestUiState()

    /**
     * Error state - Failed to load user data
     * Shows error message in overlay
     *
     * @param errorMessage Error message to display
     */
    data class Error(
        val errorMessage: String
    ) : FriendRequestUiState()
}

