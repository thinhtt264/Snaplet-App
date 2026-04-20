package com.thinh.snaplet.ui.screens.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.thinh.snaplet.data.repository.UserRepository
import com.thinh.snaplet.data.repository.chat.ChatRepository
import com.thinh.snaplet.domain.chat.LoadInitialMessagesUseCase
import com.thinh.snaplet.domain.chat.SendMessageUseCase
import com.thinh.snaplet.navigation.ChatConversation
import com.thinh.snaplet.utils.Logger
import com.thinh.snaplet.utils.Throttler
import com.thinh.snaplet.utils.network.onFailure
import com.thinh.snaplet.utils.network.onSuccess
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val PAGE_LIMIT = 15
private const val OUT_GOING_TYPING_TIMEOUT_MS = 1_500L
private const val IN_COMING_TYPING_TIMEOUT_MS = 5_000L

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val userRepository: UserRepository,
    private val loadInitialMessagesUseCase: LoadInitialMessagesUseCase,
    private val sendMessageUseCase: SendMessageUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val route = savedStateHandle.toRoute<ChatConversation>()
    val partnerName: String = route.partnerName
    val partnerAvatarUrl: String? = route.partnerAvatarUrl

    // Set immediately for an existing conversation, deferred for a new chat (recipientId flow)
    private var conversationId: String = route.conversationId ?: ""

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var typingTimeoutJob: Job? = null
    private val typingThrottler = Throttler(OUT_GOING_TYPING_TIMEOUT_MS)

    init {
        loadCurrentUser()
        observeIncomingMessages()
        observeIncomingTypingEvents()
        observeReadReceipts()

        val recipientId = route.recipientId
        if (recipientId != null) {
            createOrFindConversationAndInit(recipientId)
        } else {
            connectSocket()
            loadMessages()
        }
    }

    override fun onCleared() {
        super.onCleared()
        chatRepository.disconnectChatSocket()
    }

    fun onChangeDraftMessage(text: String) {
        _uiState.update { it.copy(draftMessage = text) }

        if (text.isNotBlank()) {
            typingThrottler.run {
                viewModelScope.launch { chatRepository.sendTypingStart(conversationId) }
            }
        }
    }

    fun onSendMessage(text: String?) {
        val trimmed = text?.trim()?.takeIf { it.isNotEmpty() } ?: return
        val currentUserId = _uiState.value.currentUserId ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(draftMessage = "") }
            typingThrottler.reset()
            chatRepository.sendTypingStop(conversationId)
            sendMessageUseCase(
                conversationId = conversationId,
                senderId = currentUserId,
                text = trimmed,
            ).collect { result ->
                when (result) {
                    is SendMessageUseCase.Result.Optimistic -> {
                        _uiState.update { state ->
                            state.copy(
                                messages = listOf(result.message) + state.messages,
                                pendingClientUuids = state.pendingClientUuids + result.message.clientUuid,
                            )
                        }
                    }

                    is SendMessageUseCase.Result.Success -> {
                        _uiState.update { state ->
                            state.copy(
                                messages = state.messages.map { msg ->
                                    if (msg.clientUuid == result.message.clientUuid) result.message else msg
                                },
                                pendingClientUuids = state.pendingClientUuids - result.message.clientUuid,
                            )
                        }
                    }

                    is SendMessageUseCase.Result.Failure -> {
                        Logger.e("sendMessage failed clientUuid=${result.clientUuid}: ${result.error}")
                        _uiState.update { state ->
                            state.copy(
                                messages = state.messages.filterNot { it.clientUuid == result.clientUuid },
                                pendingClientUuids = state.pendingClientUuids - result.clientUuid,
                            )
                        }
                    }
                }
            }
        }
    }

    fun loadMessages() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            loadInitialMessagesUseCase(conversationId = conversationId, limit = PAGE_LIMIT)
                .onSuccess { data ->
                    Logger.d("loaded ${data.data.size} messages nextCursor=${data.pagination.nextCursor}")
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            messages = data.data,
                            nextCursor = data.pagination.nextCursor,
                        )
                    }
                }
                .onFailure { error ->
                    Logger.e("loadMessages failed: ${error.message}")
                    _uiState.update { it.copy(isLoading = false, error = error.message) }
                }
        }
    }

    fun loadMore() {
        val cursor = _uiState.value.nextCursor ?: return
        if (_uiState.value.isLoadingMore) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }
            chatRepository.getMessages(
                conversationId = conversationId,
                limit = PAGE_LIMIT,
                cursor = cursor,
            )
                .onSuccess { data ->
                    Logger.d("loadMore ${data.data.size} messages nextCursor=${data.pagination.nextCursor}")
                    _uiState.update {
                        it.copy(
                            isLoadingMore = false,
                            // older messages appended to end → appear at visual top (reverseLayout = true)
                            messages = it.messages + data.data,
                            nextCursor = data.pagination.nextCursor,
                        )
                    }
                }
                .onFailure { error ->
                    Logger.e("loadMore failed: ${error.message}")
                    _uiState.update { it.copy(isLoadingMore = false) }
                }
        }
    }

    private fun createOrFindConversationAndInit(recipientId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            chatRepository.createOrFindConversation(recipientId)
                .onSuccess { data ->
                    conversationId = data.id
                    connectSocket()
                    if (data.isNew) {
                        _uiState.update { it.copy(isLoading = false) }
                    } else {
                        loadMessages()
                    }
                }
                .onFailure { error ->
                    Logger.e("createOrFindConversation failed: ${error.message}")
                    _uiState.update { it.copy(isLoading = false, error = error.message) }
                }
        }
    }

    private fun connectSocket() {
        viewModelScope.launch {
            chatRepository.connectChatSocket(conversationId)
        }
    }

    private fun loadCurrentUser() {
        viewModelScope.launch {
            val profile = userRepository.getCurrentUserProfile()
            Logger.d("currentUserId=${profile?.id}")
            _uiState.update { it.copy(currentUserId = profile?.id) }
        }
    }

    private fun observeIncomingMessages() {
        viewModelScope.launch {
            chatRepository.newMessages
                .filter { it.conversationId == conversationId }
                .collect { incoming ->
                    Logger.d("socket message id=${incoming.id}")
                    _uiState.update { state ->
                        val existsById = state.messages.any { it.id == incoming.id }
                        val optimisticIndex = state.messages.indexOfFirst {
                            it.clientUuid == incoming.clientUuid && it.id != incoming.id
                        }
                        when {
                            // Already confirmed by API — skip duplicate from socket
                            existsById -> state
                            // Optimistic entry exists — replace with server-confirmed message
                            optimisticIndex >= 0 -> state.copy(
                                messages = state.messages.toMutableList()
                                    .also { it[optimisticIndex] = incoming },
                                pendingClientUuids = state.pendingClientUuids - incoming.clientUuid,
                            )
                            // Genuinely new message from the other party
                            else -> state.copy(messages = listOf(incoming) + state.messages)
                        }
                    }
                }
        }
    }

    private fun observeIncomingTypingEvents() {
        viewModelScope.launch {
            chatRepository.typingEvents
                .filter { it.userId != _uiState.value.currentUserId }
                .collect { event ->
                    typingTimeoutJob?.cancel()
                    if (event.isTyping) {
                        _uiState.update { it.copy(isPartnerTyping = true) }
                        typingTimeoutJob = viewModelScope.launch {
                            delay(IN_COMING_TYPING_TIMEOUT_MS)
                            _uiState.update { it.copy(isPartnerTyping = false) }
                        }
                    } else {
                        _uiState.update { it.copy(isPartnerTyping = false) }
                    }
                }
        }
    }

    private fun observeReadReceipts() {
        viewModelScope.launch {
            chatRepository.readReceipts.collect { event ->
                Logger.d("read receipt userId=${event.userId} messageId=${event.messageId}")
                _uiState.update { it.copy(partnerLastReadMessageId = event.messageId) }
            }
        }
    }
}
