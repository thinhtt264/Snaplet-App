package com.thinh.snaplet.ui.screens.conversation_list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thinh.snaplet.data.repository.UserRepository
import com.thinh.snaplet.data.repository.chat.ChatRepository
import com.thinh.snaplet.utils.Logger
import com.thinh.snaplet.utils.network.onFailure
import com.thinh.snaplet.utils.network.onSuccess
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val LOG_TAG = "ConversationListVM"
private const val PAGE_LIMIT = 20

@HiltViewModel
class ConversationListViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val userRepository: UserRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConversationListUiState())
    val uiState: StateFlow<ConversationListUiState> = _uiState.asStateFlow()

    init {
        loadConversations()
        observeConversationUpdates()
    }

    fun loadConversations() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            chatRepository.getConversations(limit = PAGE_LIMIT)
                .onSuccess { data ->
                    Logger.d("$LOG_TAG: loaded ${data.data.size} conversations nextCursor=${data.pagination.nextCursor}")
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            conversations = data.data,
                            nextCursor = data.pagination.nextCursor,
                        )
                    }
                }
                .onFailure { error ->
                    Logger.e("$LOG_TAG: loadConversations failed: ${error.message}")
                    _uiState.update { it.copy(isLoading = false, error = error.message) }
                }
        }
    }

    fun loadMore() {
        val cursor = _uiState.value.nextCursor ?: return
        if (_uiState.value.isLoadingMore) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }
            chatRepository.getConversations(limit = PAGE_LIMIT, cursor = cursor)
                .onSuccess { data ->
                    Logger.d("$LOG_TAG: loadMore loaded ${data.data.size} conversations nextCursor=${data.pagination.nextCursor}")
                    _uiState.update {
                        it.copy(
                            isLoadingMore = false,
                            conversations = it.conversations + data.data,
                            nextCursor = data.pagination.nextCursor,
                        )
                    }
                }
                .onFailure { error ->
                    Logger.e("$LOG_TAG: loadMore failed: ${error.message}")
                    _uiState.update { it.copy(isLoadingMore = false) }
                }
        }
    }

    fun loadFriendList() {
        if (_uiState.value.isFriendListLoading) return
        viewModelScope.launch {
            _uiState.update { it.copy(isFriendListLoading = true, friendListError = null) }
            userRepository.getMyFriendList()
                .onSuccess { friends ->
                    _uiState.update { it.copy(isFriendListLoading = false, friendList = friends) }
                }
                .onFailure { error ->
                    Logger.e("$LOG_TAG: loadFriendList failed: ${error.message}")
                    _uiState.update { it.copy(isFriendListLoading = false, friendListError = error.message) }
                }
        }
    }

    // ── Private ───────────────────────────────────────────────────────────

    private fun observeConversationUpdates() {
        viewModelScope.launch {
            val currentUserId = userRepository.getCurrentUserProfile()?.id
            chatRepository.conversationUpdates.collect { event ->
                Logger.d("$LOG_TAG: conversation updated conversationId=${event.conversationId}")
                _uiState.update { state ->
                    val updatedList = state.conversations.toMutableList()
                    val index = updatedList.indexOfFirst { it.id == event.conversationId }

                    val isIncoming = event.lastMessage?.senderId != null &&
                            event.lastMessage.senderId != currentUserId

                    if (index >= 0) {
                        val existing = updatedList[index]
                        val updated = existing.copy(
                            lastMessage = event.lastMessage,
                            lastMessageAt = event.lastMessage?.createdAt ?: existing.lastMessageAt,
                            hasUnread = if (isIncoming) true else existing.hasUnread,
                        )
                        // Move updated conversation to top
                        updatedList.removeAt(index)
                        updatedList.add(0, updated)
                    }
                    // If not in list yet, ignore — next loadConversations() will pick it up
                    state.copy(conversations = updatedList)
                }
            }
        }
    }
}
