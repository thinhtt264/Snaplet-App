package com.thinh.snaplet.ui.screens.friend_request

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thinh.snaplet.data.repository.UserRepository
import com.thinh.snaplet.utils.Logger
import com.thinh.snaplet.utils.deeplink.DeepLinkEvent
import com.thinh.snaplet.utils.deeplink.DeepLinkManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FriendRequestViewModel @Inject constructor(
    private val deepLinkManager: DeepLinkManager,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<FriendRequestUiState>(
        FriendRequestUiState.Hidden
    )
    val uiState: StateFlow<FriendRequestUiState> = _uiState.asStateFlow()

    init {
        observeDeepLinkEvents()
    }

    private fun observeDeepLinkEvents() {
        viewModelScope.launch {
            deepLinkManager.events.collect { event ->
                Logger.d("📨 FriendRequestViewModel: Received event: $event")
                if (event is DeepLinkEvent.FriendRequest && _uiState.value is FriendRequestUiState.Hidden) {
                    handleFriendRequestEvent(event.userName)
                }
            }
        }
    }

    /**
     * Handle friend request deeplink event Fetches user profile and updates UI
     * state
     *
     * @param userName Username from deeplink
     */
    private suspend fun handleFriendRequestEvent(userName: String) {
        // Show loading state
        _uiState.value = FriendRequestUiState.Loading(userName)

        // Fetch user profile from repository
        val result = userRepository.getUserProfile(userName)

        // Update UI state based on result
        _uiState.value = result.fold(
            onSuccess = { userProfile ->
                Logger.d("✅ User profile loaded, showing overlay")
                FriendRequestUiState.Visible(
                    userProfile = userProfile,
                    isLoading = false
                )
            },
            onFailure = { error ->
                Logger.e("❌ Failed to load user profile: ${error.message}")
                FriendRequestUiState.Error(
                    userName = userName,
                    errorMessage = error.message ?: "Unknown error"
                )
            }
        )
    }

    /** Handle resend friend request action Calls repository and handles result */
    fun onResendFriendRequest() {
        viewModelScope.launch {
            val currentState = _uiState.value
            if (currentState is FriendRequestUiState.Visible) {
                // Set loading state
                _uiState.value = currentState.copy(isLoading = true)

                val userName = currentState.userProfile.userName
                Logger.d("🔄 Resending friend request to: $userName")

                // Call repository
                val result = userRepository.sendFriendRequest(userName)

                result.fold(
                    onSuccess = {
                        Logger.d("✅ Friend request sent successfully")
                        // Hide overlay after successful action
                        _uiState.value = FriendRequestUiState.Hidden
                    },
                    onFailure = { error ->
                        Logger.e("❌ Failed to send friend request: ${error.message}")
                        // Reset loading state on error
                        _uiState.value = currentState.copy(isLoading = false)
                        // TODO: Show error message to user
                    }
                )
            }
        }
    }

    /** Handle dismiss overlay action */
    fun onDismiss() {
        Logger.d("❌ Dismissing friend request overlay")
        _uiState.value = FriendRequestUiState.Hidden
    }
}

