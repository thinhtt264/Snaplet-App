package com.thinh.snaplet.ui.screens.conversation_list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thinh.snaplet.data.local.entity.toUiModel
import com.thinh.snaplet.data.repository.UserRepository
import com.thinh.snaplet.data.repository.chat.ChatRepository
import com.thinh.snaplet.domain.chat.OnlinePresenceController
import com.thinh.snaplet.platform.network.ConnectivityObserver
import com.thinh.snaplet.utils.Logger
import com.thinh.snaplet.utils.network.onFailure
import com.thinh.snaplet.utils.network.onSuccess
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val PAGE_LIMIT = 20
private const val SYNC_DEBOUNCE_MS = 3_000L

@HiltViewModel
class ConversationListViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val userRepository: UserRepository,
    private val connectivityObserver: ConnectivityObserver,
    private val onlinePresenceController: OnlinePresenceController,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConversationListUiState())
    private var lastSyncAt = 0L

    val uiState: StateFlow<ConversationListUiState> = combine(
        _uiState,
        userRepository.observeMyUserProfile().distinctUntilChanged(),
        chatRepository.observeConversations(),
        chatRepository.observeLastMessageStatuses(),
        onlinePresenceController.onlineUserIds,
    ) { state, profileUi, entities, lastMessageStatuses, onlineUserIds ->
        val myUserId = profileUi?.id
        val lastStatusByConversationId = lastMessageStatuses.associate { it.conversationId to it.status }
        state.copy(
            userProfile = profileUi,
            conversations = entities.map { entity ->
                entity.toUiModel(
                    myUserId = myUserId,
                    lastMessageStatus = lastStatusByConversationId[entity.id],
                    isPartnerOnline = onlineUserIds.contains(entity.participantId),
                )
            },
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = _uiState.value,
    )

    init {
        syncOnResume()
        observeNetworkReconnect()
    }

    fun onScreenResumed() {
        syncOnResume()
    }

    fun loadMore() {
        val cursor = _uiState.value.nextCursor ?: return
        if (_uiState.value.isLoadingMore) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }
            chatRepository.getConversations(limit = PAGE_LIMIT, cursor = cursor).onSuccess { data ->
                _uiState.update {
                    it.copy(
                        isLoadingMore = false,
                        nextCursor = data.pagination.nextCursor,
                    )
                }
            }.onFailure { error ->
                Logger.e("conversation list loadMore failed: ${error.message}")
                _uiState.update { it.copy(isLoadingMore = false) }
            }
        }
    }

    fun loadFriendList() {
        if (_uiState.value.isFriendListLoading) return
        viewModelScope.launch {
            _uiState.update { it.copy(isFriendListLoading = true, friendListError = null) }
            userRepository.getMyFriendList().onSuccess { friends ->
                _uiState.update { it.copy(isFriendListLoading = false, friendList = friends) }
            }.onFailure { error ->
                Logger.e("conversation list loadFriendList failed: ${error.message}")
                _uiState.update {
                    it.copy(isFriendListLoading = false, friendListError = error.message)
                }
            }
        }
    }

    private fun syncOnResume() {
        val now = System.currentTimeMillis()
        if (now - lastSyncAt < SYNC_DEBOUNCE_MS) return
        lastSyncAt = now
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            chatRepository.syncConversations()
                .onFailure { error ->
                    Logger.e("conversation list syncConversations failed: ${error.message}")
                    _uiState.update { it.copy(error = error.message) }
                }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    private fun observeNetworkReconnect() {
        viewModelScope.launch {
            connectivityObserver.isInternetAvailable.filter { it }.drop(1).collect {
                chatRepository.syncConversations()
            }
        }
    }
}
