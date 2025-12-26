package com.thinh.snaplet.ui.screens.friend_request

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thinh.snaplet.data.repository.UserRepository
import com.thinh.snaplet.utils.Logger
import com.thinh.snaplet.utils.deeplink.DeepLinkEvent
import com.thinh.snaplet.utils.deeplink.DeepLinkManager
import com.thinh.snaplet.utils.safeMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FriendRequestViewModel @Inject constructor(
    private val deepLinkManager: DeepLinkManager, private val userRepository: UserRepository
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

    private suspend fun handleFriendRequestEvent(userName: String) {
        _uiState.value = FriendRequestUiState.Loading(userName)

        val result = userRepository.getUserProfile(userName)

        _uiState.value = result.fold(onSuccess = { userProfile ->
            Logger.d("✅ User profile loaded, showing overlay")
            FriendRequestUiState.Visible(userProfile = userProfile)
        }, onFailure = { error ->
            val errorMessage = error.safeMessage
            Logger.e("❌ Failed to load user profile: $errorMessage")
            FriendRequestUiState.Error(errorMessage = errorMessage)
        })
    }

    fun onSendFriendRequest() {
        viewModelScope.launch {
            val currentState = _uiState.value
            if (currentState is FriendRequestUiState.Visible) {
                _uiState.value = currentState.copy(isLoading = true)

                val userId = currentState.userProfile.id

                val result = userRepository.sendFriendRequest(userId)

                result.fold(onSuccess = { relationship ->
                    Logger.d("✅ Friend request sent successfully - Relationship: ${relationship.id}, Status: ${relationship.status}")
                    _uiState.value = FriendRequestUiState.Hidden
                }, onFailure = { error ->
                    Logger.e("❌ Failed to send friend request: ${error.message}")
                    _uiState.value = currentState.copy(isLoading = false)
                })
            }
        }
    }

    fun onDismiss() {
        Logger.d("❌ Dismissing friend request overlay")
        _uiState.value = FriendRequestUiState.Hidden
    }
}

