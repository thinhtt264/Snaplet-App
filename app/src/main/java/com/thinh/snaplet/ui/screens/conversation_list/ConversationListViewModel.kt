package com.thinh.snaplet.ui.screens.conversation_list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thinh.snaplet.data.model.chat.Conversation
import com.thinh.snaplet.data.repository.UserRepository
import com.thinh.snaplet.data.repository.chat.ChatRepository
import com.thinh.snaplet.utils.Logger
import com.thinh.snaplet.utils.isGreaterWithFallback
import com.thinh.snaplet.utils.network.onFailure
import com.thinh.snaplet.utils.network.onSuccess
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
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

    val uiState: StateFlow<ConversationListUiState> = combine(
        _uiState,
        userRepository.observeMyUserProfile().distinctUntilChanged(),
    ) { state, profileUi ->
        state.copy(userProfile = profileUi)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = _uiState.value
    )

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
                            conversations = data.data.map { conv -> conv.toUiItem(it.userProfile?.id) },
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
            val currentUserId = userRepository.getCurrentUserProfile()?.id
            chatRepository.getConversations(limit = PAGE_LIMIT, cursor = cursor)
                .onSuccess { data ->
                    Logger.d("$LOG_TAG: loadMore loaded ${data.data.size} conversations nextCursor=${data.pagination.nextCursor}")
                    _uiState.update {
                        it.copy(
                            isLoadingMore = false,
                            conversations = it.conversations + data.data.map { conv ->
                                conv.toUiItem(
                                    currentUserId
                                )
                            },
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
                    _uiState.update {
                        it.copy(
                            isFriendListLoading = false,
                            friendListError = error.message
                        )
                    }
                }
        }
    }

    private fun observeConversationUpdates() {
        viewModelScope.launch {
            chatRepository.conversationUpdates.collect { event ->
                val currentState = _uiState.value
                val index =
                    currentState.conversations.indexOfFirst { it.conversation.id == event.conversationId }

                if (index >= 0) {
                    val currentUserId = userRepository.getCurrentUserProfile()?.id
                    _uiState.update { state ->
                        val updatedList = state.conversations.toMutableList()
                        val updated = updatedList[index].conversation.copy(
                            lastMessage = event.lastMessage,
                            myLastReadAt = event.myLastReadAt,
                            partnerLastReadAt = event.partnerLastReadAt,
                        )
                        updatedList.removeAt(index)
                        updatedList.add(0, updated.toUiItem(currentUserId))
                        state.copy(conversations = updatedList)
                    }
                } else {
                    loadConversations()
                }
            }
        }
    }
}

private fun Conversation.toUiItem(currentUserId: String?): ConversationUiItem {
    val isUnread = lastMessage != null &&
            lastMessage.senderId != currentUserId &&
            isGreaterWithFallback(lastMessage.createdAt, myLastReadAt, fallback = true)
    return ConversationUiItem(conversation = this, isUnread = isUnread)
}
