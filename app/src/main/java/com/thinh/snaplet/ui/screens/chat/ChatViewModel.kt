package com.thinh.snaplet.ui.screens.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import com.thinh.snaplet.data.local.entity.MessageStatus
import com.thinh.snaplet.data.local.entity.toMessage
import com.thinh.snaplet.data.model.chat.Message
import com.thinh.snaplet.data.repository.UserRepository
import com.thinh.snaplet.data.repository.chat.ChatRepository
import com.thinh.snaplet.domain.chat.LoadInitialMessagesUseCase
import com.thinh.snaplet.domain.chat.MarkMessageSeenUseCase
import com.thinh.snaplet.navigation.ChatConversation
import com.thinh.snaplet.platform.network.ConnectivityObserver
import com.thinh.snaplet.utils.Logger
import com.thinh.snaplet.utils.Throttler
import com.thinh.snaplet.utils.network.onFailure
import com.thinh.snaplet.utils.network.onSuccess
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val OUT_GOING_TYPING_TIMEOUT_MS = 1_500L
private const val IN_COMING_TYPING_TIMEOUT_MS = 3_000L
private const val MARK_READ_DEBOUNCE_MS = 500L

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val userRepository: UserRepository,
    private val loadInitialMessagesUseCase: LoadInitialMessagesUseCase,
    private val markMessageSeenUseCase: MarkMessageSeenUseCase,
    private val connectivityObserver: ConnectivityObserver,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val route = savedStateHandle.toRoute<ChatConversation>()
    val partnerName: String = route.partnerName
    val partnerAvatarUrl: String? = route.partnerAvatarUrl

    private var conversationId: String = route.conversationId ?: ""
    private val _activeConversationId = MutableStateFlow(route.conversationId ?: "")

    private val _uiState = MutableStateFlow(
        ChatUiState(
            partner = PartnerState(lastReadAtMsFallback = route.partnerLastReadAtMs),
            readTracking = ReadTrackingState(myLastReadCreatedAtMs = route.myLastReadAtMs),
        )
    )
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val messages: Flow<PagingData<Message>> = _activeConversationId
        .filter { it.isNotEmpty() }
        .flatMapLatest { convId ->
            chatRepository.getMessagesPager(convId)
                .map { pagingData -> pagingData.map { it.toMessage() } }
        }
        .cachedIn(viewModelScope)

    private var typingTimeoutJob: Job? = null
    private var markReadJob: Job? = null
    private val typingThrottler = Throttler(OUT_GOING_TYPING_TIMEOUT_MS)

    private var isInForeground = false

    init {
        loadCurrentUser()
        observeIncomingTypingEvents()
        observeIncomingReadReceipts()
        observeConnectivity()
        observeNetworkReconnect()

        val recipientId = route.recipientId
        if (recipientId != null) {
            createOrFindConversationAndInit(recipientId)
        } else if (conversationId.isNotEmpty()) {
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
        _uiState.update { it.copy(draftMessage = "") }
        typingThrottler.reset()
        viewModelScope.launch {
            chatRepository.sendTextMessage(conversationId, currentUserId, trimmed)
        }
        viewModelScope.launch { chatRepository.sendTypingStop(conversationId) }
    }

    fun loadMessages() {
        if (conversationId.isEmpty()) return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    messageList = it.messageList.copy(
                        isLoading = true,
                        error = null
                    )
                )
            }
            loadInitialMessagesUseCase(conversationId = conversationId)
                .onSuccess {
                    _uiState.update { it.copy(messageList = it.messageList.copy(isLoading = false)) }
                }
                .onFailure { error ->
                    Logger.e("loadMessages failed: ${error.message}")
                    _uiState.update {
                        it.copy(
                            messageList = it.messageList.copy(
                                isLoading = false,
                                error = error.message
                            )
                        )
                    }
                }
        }
    }

    private fun createOrFindConversationAndInit(recipientId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(messageList = it.messageList.copy(isLoading = true)) }
            chatRepository.createOrFindConversation(recipientId).onSuccess { data ->
                conversationId = data.id
                _activeConversationId.value = data.id
                connectSocket()
                loadMessages()
            }.onFailure { error ->
                Logger.e("createOrFindConversation failed: ${error.message}")
                _uiState.update {
                    it.copy(
                        messageList = it.messageList.copy(
                            isLoading = false,
                            error = error.message
                        )
                    )
                }
            }
        }
    }

    private fun connectSocket() {
        viewModelScope.launch { chatRepository.connectChatSocket(conversationId) }
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

    private fun observeNetworkReconnect() {
        viewModelScope.launch {
            connectivityObserver.isInternetAvailable
                .filter { it }
                .drop(1)
                .collect {
                    if (conversationId.isNotEmpty()) {
                        chatRepository.syncOnReconnect(conversationId)
                    }
                }
        }
    }

    private fun loadCurrentUser() {
        viewModelScope.launch {
            val profile = userRepository.getCurrentUserProfile()
            Logger.d("currentUserId=${profile?.id}")
            _uiState.update { it.copy(currentUserId = profile?.id) }
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
            triggerMarkSeen(newestMessageId, System.currentTimeMillis())
        }
        _uiState.update { it.copy(readTracking = it.readTracking.copy(incomingUnread = IncomingUnreadState())) }
        Logger.d("incoming unread banner reset")
    }

    fun onVisibleMessagesChanged(visibleMessages: List<Message>) {
        markReadJob?.cancel()
        markReadJob = viewModelScope.launch {
            delay(MARK_READ_DEBOUNCE_MS)
            val newest = visibleMessages
                .filter { it.status == null || it.status == MessageStatus.SENT }
                .maxByOrNull { it.createdAt.time } ?: return@launch
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
                _uiState.update {
                    if (it.currentUserId == event.userId) return@collect
                    it.copy(partner = it.partner.copy(lastReadEvent = event))
                }
            }
        }
    }
}
