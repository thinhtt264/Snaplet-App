package com.thinh.snaplet.ui.screens.friend_request

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thinh.snaplet.data.model.user.UserProfile
import com.thinh.snaplet.data.repository.UserRepository
import com.thinh.snaplet.domain.model.RelationshipAction
import com.thinh.snaplet.domain.user.GetRelationshipActionUseCase
import com.thinh.snaplet.ui.overlay.OverlayEventBus
import com.thinh.snaplet.utils.Logger
import com.thinh.snaplet.utils.network.onFailure
import com.thinh.snaplet.utils.network.onSuccess
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FriendRequestViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val getRelationshipActionUseCase: GetRelationshipActionUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FriendRequestUiState(userProfile = null))
    val uiState: StateFlow<FriendRequestUiState> = _uiState.asStateFlow()

    fun loadUser(userProfile: UserProfile?) {
        if (userProfile == null) return

        viewModelScope.launch {
            _uiState.value = FriendRequestUiState(userProfile = userProfile, isLoading = true)

            val relationshipAction = getRelationshipActionUseCase(userProfile.id)

            _uiState.update {
                it.copy(
                    relationshipAction = relationshipAction,
                    isLoading = false
                )
            }
        }
    }

    fun sendFriendRequest() {
        val targetUserId = _uiState.value.userProfile?.id ?: return
        if (_uiState.value.isRequesting) return

        viewModelScope.launch {
            _uiState.update { it.copy(isRequesting = true) }
            userRepository.sendFriendRequest(targetUserId)
                .onSuccess {
                    loadUser(_uiState.value.userProfile)
                    _uiState.update { it.copy(isRequesting = false) }
                }
                .onFailure { error ->
                    Logger.e("❌ Failed to send friend request: ${error.message}")
                    _uiState.update { it.copy(isRequesting = false) }
                }
        }
    }

    fun refreshPending() {
        if (_uiState.value.isRequesting) return
        viewModelScope.launch {
            _uiState.update { it.copy(isRequesting = true) }
            delay(1500L)
            _uiState.update { it.copy(isRequesting = false) }
        }
    }

    fun acceptFriendRequest() {
        val relationshipId =
            (_uiState.value.relationshipAction as? RelationshipAction.PendingByOther)?.relationshipId
                ?: return
        if (_uiState.value.isRequesting) return

        viewModelScope.launch {
            _uiState.update { it.copy(isRequesting = true) }
            userRepository.acceptFriendRequest(relationshipId)
                .onSuccess {
                    loadUser(_uiState.value.userProfile)
                    _uiState.update { it.copy(isRequesting = false) }
                }
                .onFailure { error ->
                    Logger.e("❌ Failed to accept friend request: ${error.message}")
                    _uiState.update { it.copy(isRequesting = false) }
                }
        }
    }

    fun dismiss() {
        resetState()
        OverlayEventBus.dismiss()
    }

    fun resetState() {
        _uiState.value = FriendRequestUiState(userProfile = null)
    }
}