package com.thinh.snaplet.ui.screens.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.thinh.snaplet.data.model.chat.Message
import com.thinh.snaplet.data.repository.UserRepository
import com.thinh.snaplet.data.repository.chat.ChatRepository
import com.thinh.snaplet.domain.chat.LoadInitialMessagesUseCase
import com.thinh.snaplet.domain.chat.MarkMessageSeenUseCase
import com.thinh.snaplet.domain.chat.ObserveNewMessageUseCase
import com.thinh.snaplet.domain.chat.SendMessageUseCase
import com.thinh.snaplet.navigation.ChatConversation
import com.thinh.snaplet.platform.network.ConnectivityObserver
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
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val PAGE_LIMIT = 15
private const val OUT_GOING_TYPING_TIMEOUT_MS = 1_500L
private const val IN_COMING_TYPING_TIMEOUT_MS = 3_000L

private const val MARK_READ_DEBOUNCE_MS = 500L

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val userRepository: UserRepository,
    private val loadInitialMessagesUseCase: LoadInitialMessagesUseCase,
    private val sendMessageUseCase: SendMessageUseCase,
    private val observeNewMessageUseCase: ObserveNewMessageUseCase,
    private val markMessageSeenUseCase: MarkMessageSeenUseCase,
    private val connectivityObserver: ConnectivityObserver,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val route = savedStateHandle.toRoute<ChatConversation>()
    val partnerName: String = route.partnerName
    val partnerAvatarUrl: String? = route.partnerAvatarUrl

    private var conversationId: String = route.conversationId ?: ""

    private val _uiState = MutableStateFlow(
        ChatUiState(
            partner = PartnerState(lastReadAtMsFallback = route.partnerLastReadAtMs),
            readTracking = ReadTrackingState(myLastReadCreatedAtMs = route.myLastReadAtMs),
        )
    )
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var typingTimeoutJob: Job? = null
    private var markReadJob: Job? = null
    private val typingThrottler = Throttler(OUT_GOING_TYPING_TIMEOUT_MS)

    private var isInForeground = false

    init {
        loadCurrentUser()
        observeIncomingMessages()
        observeIncomingTypingEvents()
        observeIncomingReadReceipts()
        observeConnectivity()

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

    fun onResume() {
        isInForeground = true
        if (conversationId.isNotEmpty() && connectivityObserver.isInternetAvailable.value) {
            connectSocket()
        }
    }

    fun onPause() {
        isInForeground = false
        typingThrottler.reset()
        viewModelScope.launch { chatRepository.sendTypingStop(conversationId) }
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
                                messageList = state.messageList.copy(
                                    messages = listOf(result.message) + state.messageList.messages,
                                ),
                                pendingClientUuids = state.pendingClientUuids + result.message.clientUuid,
                            )
                        }
                    }

                    is SendMessageUseCase.Result.Success -> {
                        _uiState.update { state ->
                            state.copy(
                                messageList = state.messageList.copy(
                                    messages = state.messageList.messages.map { msg ->
                                        if (msg.clientUuid == result.message.clientUuid) result.message else msg
                                    },
                                ),
                                pendingClientUuids = state.pendingClientUuids - result.message.clientUuid,
                            )
                        }
                    }

                    is SendMessageUseCase.Result.Failure -> {
                        Logger.e("sendMessage failed clientUuid=${result.clientUuid}: ${result.error}")
                        _uiState.update { state ->
                            state.copy(
                                pendingClientUuids = state.pendingClientUuids - result.clientUuid,
                                errorClientUuids = state.errorClientUuids + result.clientUuid,
                            )
                        }
                    }
                }
            }
        }
    }

    fun loadMessages() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    messageList = it.messageList.copy(
                        isLoading = true, error = null
                    )
                )
            }
            loadInitialMessagesUseCase(
                conversationId = conversationId, limit = PAGE_LIMIT
            ).onSuccess { data ->
                Logger.d("loaded ${data.data.size} messages nextCursor=${data.pagination.nextCursor}")
                _uiState.update {
                    it.copy(
                        messageList = it.messageList.copy(
                            isLoading = false,
                            messages = data.data,
                            nextCursor = data.pagination.nextCursor,
                        )
                    )
                }
            }.onFailure { error ->
                Logger.e("loadMessages failed: ${error.message}")
                _uiState.update {
                    it.copy(
                        messageList = it.messageList.copy(
                            isLoading = false, error = error.message
                        )
                    )
                }
            }
        }
    }

    fun loadMore() {
        val cursor = _uiState.value.messageList.nextCursor ?: return
        if (_uiState.value.messageList.isLoadingMore) return

        viewModelScope.launch {
            _uiState.update { it.copy(messageList = it.messageList.copy(isLoadingMore = true)) }
            chatRepository.getMessages(
                conversationId = conversationId,
                limit = PAGE_LIMIT,
                cursor = cursor,
            ).onSuccess { data ->
                Logger.d("loadMore ${data.data.size} messages nextCursor=${data.pagination.nextCursor}")
                _uiState.update {
                    it.copy(
                        messageList = it.messageList.copy(
                            isLoadingMore = false,
                            messages = it.messageList.messages + data.data,
                            nextCursor = data.pagination.nextCursor,
                        )
                    )
                }
            }.onFailure { error ->
                Logger.e("loadMore failed: ${error.message}")
                _uiState.update { it.copy(messageList = it.messageList.copy(isLoadingMore = false)) }
            }
        }
    }

    private fun createOrFindConversationAndInit(recipientId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(messageList = it.messageList.copy(isLoading = true)) }
            chatRepository.createOrFindConversation(recipientId).onSuccess { data ->
                conversationId = data.id
                connectSocket()
                if (data.isNew) {
                    _uiState.update { it.copy(messageList = it.messageList.copy(isLoading = false)) }
                } else {
                    loadMessages()
                }
            }.onFailure { error ->
                Logger.e("createOrFindConversation failed: ${error.message}")
                _uiState.update {
                    it.copy(
                        messageList = it.messageList.copy(
                            isLoading = false, error = error.message
                        )
                    )
                }
            }
        }
    }

    private fun connectSocket() {
        viewModelScope.launch {
            chatRepository.connectChatSocket(conversationId)
        }
    }

    private fun observeConnectivity() {
        viewModelScope.launch {
            connectivityObserver.isInternetAvailable.onEach { isAvailable ->
                if (isAvailable && isInForeground && conversationId.isNotEmpty()) {
                    connectSocket()
                }
            }.collect()
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
            observeNewMessageUseCase().filter { it.conversationId == conversationId }
                .collect { incoming ->
                    Logger.d("socket message id=${incoming.id}")
                    val snapshot = _uiState.value
                    val existsById = snapshot.messageList.messages.any { it.id == incoming.id }
                    val optimisticIndex = snapshot.messageList.messages.indexOfFirst {
                        it.clientUuid == incoming.clientUuid && it.id != incoming.id
                    }
                    val isGenuinelyNew = !existsById && optimisticIndex < 0

                    _uiState.update { state ->
                        val existsByIdInState =
                            state.messageList.messages.any { it.id == incoming.id }
                        val optimisticIndexInState = state.messageList.messages.indexOfFirst {
                            it.clientUuid == incoming.clientUuid && it.id != incoming.id
                        }
                        when {
                            existsByIdInState -> state
                            optimisticIndexInState >= 0 -> state.copy(
                                messageList = state.messageList.copy(
                                    messages = state.messageList.messages.toMutableList()
                                        .also { it[optimisticIndexInState] = incoming },
                                ),
                                pendingClientUuids = state.pendingClientUuids - incoming.clientUuid,
                            )

                            else -> state.copy(
                                messageList = state.messageList.copy(
                                    messages = listOf(incoming) + state.messageList.messages,
                                )
                            )
                        }
                    }

                    if (isGenuinelyNew) {
                        if (_uiState.value.readTracking.isUserAtBottom) {
                            Logger.d("new message, user at bottom → mark read id=${incoming.id}")
                            triggerMarkSeen(incoming.id, incoming.createdAt.time)
                        } else {
                            _uiState.update { state ->
                                val current = state.readTracking.incomingUnread
                                val newCount = (current.count + 1).coerceAtMost(10)
                                Logger.d("accumulate unread: count=$newCount newestId=${incoming.id}")
                                state.copy(
                                    readTracking = state.readTracking.copy(
                                        incomingUnread = current.copy(
                                            count = newCount, newestMessageId = incoming.id
                                        ),
                                    )
                                )
                            }
                        }
                    }
                }
        }
    }

    fun onIsAtBottomChanged(isAtBottom: Boolean) {
        val wasAtBottom = _uiState.value.readTracking.isUserAtBottom
        _uiState.update { it.copy(readTracking = it.readTracking.copy(isUserAtBottom = isAtBottom)) }
        Logger.d("isUserAtBottom=$isAtBottom")
        if (isAtBottom && !wasAtBottom) onUserScrolledToBottom()
    }

    fun onUserScrolledToBottom() {
        val state = _uiState.value
        val newestMessageId = state.readTracking.incomingUnread.newestMessageId
        if (newestMessageId != null) {
            val msg = state.messageList.messages.firstOrNull { it.id == newestMessageId }
            if (msg != null) {
                Logger.d("user scrolled to bottom → mark read from banner id=$newestMessageId")
                triggerMarkSeen(newestMessageId, msg.createdAt.time)
            }
        }
        _uiState.update { it.copy(readTracking = it.readTracking.copy(incomingUnread = IncomingUnreadState())) }
        Logger.d("incoming unread banner reset")
    }

    fun onVisibleMessagesChanged(visibleMessages: List<Message>) {
        markReadJob?.cancel()
        markReadJob = viewModelScope.launch {
            delay(MARK_READ_DEBOUNCE_MS)
            val newest = visibleMessages.maxByOrNull { it.createdAt.time } ?: return@launch
            val newestEpoch = newest.createdAt.time
            val myLastReadMs = _uiState.value.readTracking.myLastReadCreatedAtMs
            if (myLastReadMs == null || newestEpoch > myLastReadMs) {
                Logger.d("onVisibleMessagesChanged → mark read id=${newest.id} epoch=$newestEpoch")
                triggerMarkSeen(newest.id, newestEpoch)
            }
        }
    }

    private fun triggerMarkSeen(messageId: String, createdAtMs: Long) {
        viewModelScope.launch { markMessageSeenUseCase(conversationId, messageId) }
        _uiState.update { it.copy(readTracking = it.readTracking.copy(myLastReadCreatedAtMs = createdAtMs)) }
        Logger.d("triggerMarkRead: messageId=$messageId epoch=$createdAtMs")
    }

    private fun observeIncomingTypingEvents() {
        viewModelScope.launch {
            chatRepository.typingEvents.filter { it.userId != _uiState.value.currentUserId }
                .collect { event ->
                    typingTimeoutJob?.cancel()
                    if (event.isTyping) {
                        _uiState.update { it.copy(partner = it.partner.copy(isTyping = true)) }
                        typingTimeoutJob = viewModelScope.launch {
                            delay(IN_COMING_TYPING_TIMEOUT_MS)
                            _uiState.update { it.copy(partner = it.partner.copy(isTyping = false)) }
                        }
                    } else {
                        _uiState.update { it.copy(partner = it.partner.copy(isTyping = false)) }
                    }
                }
        }
    }

    private fun observeIncomingReadReceipts() {
        viewModelScope.launch {
            chatRepository.readReceipts.collect { event ->
                Logger.d("read receipt userId=${event.userId} messageId=${event.messageId}")
                _uiState.update {
                    if (it.currentUserId == event.userId) return@collect
                    it.copy(partner = it.partner.copy(lastReadEvent = event))
                }
            }
        }
    }
}
