package com.thinh.snaplet.ui.screens.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.thinh.snaplet.data.local.entity.MessageEntity
import com.thinh.snaplet.data.local.entity.MessageStatus
import com.thinh.snaplet.data.model.chat.Message
import com.thinh.snaplet.data.repository.UserRepository
import com.thinh.snaplet.data.repository.chat.ChatRepository
import com.thinh.snaplet.data.repository.quickchat.QuickChatEmojiRepository
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val OUT_GOING_TYPING_TIMEOUT_MS = 1_500L
private const val IN_COMING_TYPING_TIMEOUT_MS = 3_000L
private const val MARK_READ_DEBOUNCE_MS = 500L
private const val SYNC_DEBOUNCE_MS = 5_000L
private const val CHAT_RECENT_MAX_SLOTS = 4
private val CHAT_RECENT_DEFAULT_EMOJIS = listOf("😀", "😂", "😮", "👍")

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val userRepository: UserRepository,
    private val quickChatEmojiRepository: QuickChatEmojiRepository,
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
    val messages: Flow<PagingData<MessageEntity>> = _activeConversationId
        .filter { it.isNotEmpty() }
        .flatMapLatest { convId -> chatRepository.getMessagesPager(convId) }
        .cachedIn(viewModelScope)

    private var typingTimeoutJob: Job? = null
    private var markReadJob: Job? = null
    private val typingThrottler = Throttler(OUT_GOING_TYPING_TIMEOUT_MS)

    private var lastSyncAt = 0L

    init {
        loadCurrentUser()
        observeIncomingTypingEvents()
        observeIncomingReadReceipts()
        observeNetworkReconnect()
        loadRecentEmojis()

        if (conversationId.isNotEmpty()) {
            syncOnResume()
        }
    }

    override fun onCleared() {
        super.onCleared()
        chatRepository.disconnectChatSocket()
    }

    fun onResume() {
        syncOnResume()
    }

    fun onPause() {
        typingThrottler.reset()
        viewModelScope.launch { chatRepository.sendTypingStop(conversationId) }
        chatRepository.disconnectChatSocket()
    }

    fun onChangeDraftMessage(text: String) {
        _uiState.update { it.copy(draftMessage = text) }
        if (text.isNotBlank() && conversationId.isNotEmpty()) {
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
            if (conversationId.isEmpty()) {
                val recipientId = route.recipientId ?: return@launch
                sendFirstMessageAndInit(recipientId, currentUserId, trimmed)
            } else {
                chatRepository.sendTextMessage(conversationId, currentUserId, trimmed)
                chatRepository.sendTypingStop(conversationId)
            }
        }
    }

    fun onMessageLongPress(message: MessageEntity) {
        _uiState.update { it.copy(inspectedMessage = message) }
    }

    fun dismissInspect() {
        _uiState.update { it.copy(inspectedMessage = null) }
    }

    fun onRecentEmojiUsed(emoji: String) {
        viewModelScope.launch {
            quickChatEmojiRepository.recordEmojiUsage(emoji)
            loadRecentEmojis()
        }
    }

    private suspend fun sendFirstMessageAndInit(
        recipientId: String,
        senderId: String,
        text: String
    ) {
        _uiState.update { it.copy(messageList = it.messageList.copy(isLoading = true)) }
        chatRepository.sendFirstMessage(recipientId, senderId, text)
            .onSuccess { message ->
                conversationId = message.conversationId
                _activeConversationId.value = message.conversationId
                _uiState.update { it.copy(messageList = it.messageList.copy(isLoading = false)) }
                viewModelScope.launch { connectSocketAndSyncMessages() }
            }
            .onFailure { error ->
                Logger.e("sendFirstMessage failed: ${error.message}")
                _uiState.update {
                    it.copy(
                        draftMessage = text,
                        messageList = it.messageList.copy(isLoading = false, error = error.message),
                    )
                }
            }
    }

    fun loadMessages() {
        if (conversationId.isEmpty()) return
        viewModelScope.launch { connectSocketAndSyncMessages() }
    }

    private fun syncOnResume() {
        if (conversationId.isEmpty()) return
        val now = System.currentTimeMillis()
        if (now - lastSyncAt < SYNC_DEBOUNCE_MS) return
        lastSyncAt = now
        viewModelScope.launch { connectSocketAndSyncMessages() }
    }

    private suspend fun connectSocketAndSyncMessages() {
        if (conversationId.isEmpty()) return
        _uiState.update {
            it.copy(
                messageList = it.messageList.copy(isLoading = false, error = null),
            )
        }
        chatRepository.connectChatSocket(conversationId)
        chatRepository.syncOnReconnect(conversationId)
    }

    private fun observeNetworkReconnect() {
        viewModelScope.launch {
            connectivityObserver.isInternetAvailable.filter { it }.drop(1).collect {
                if (conversationId.isNotEmpty()) {
                    connectSocketAndSyncMessages()
                }
            }
        }
    }

    private fun loadCurrentUser() {
        viewModelScope.launch {
            val profile = userRepository.getCurrentUserProfile()
            _uiState.update { it.copy(currentUserId = profile?.id) }
        }
    }

    fun onIsAtBottomChanged(isAtBottom: Boolean) {
        val wasAtBottom = _uiState.value.readTracking.isUserAtBottom
        _uiState.update { it.copy(readTracking = it.readTracking.copy(isUserAtBottom = isAtBottom)) }
        if (isAtBottom && !wasAtBottom) onUserScrolledToBottom()
    }

    fun onUserScrolledToBottom() {
        val state = _uiState.value
        val newestMessageId = state.readTracking.incomingUnread.newestMessageId
        if (newestMessageId != null) {
            triggerMarkSeen(newestMessageId, System.currentTimeMillis())
        }
        _uiState.update { it.copy(readTracking = it.readTracking.copy(incomingUnread = IncomingUnreadState())) }
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
                triggerMarkSeen(newest.id, newestEpoch)
            }
        }
    }

    private fun triggerMarkSeen(messageId: String, createdAtMs: Long) {
        markMessageSeenUseCase(conversationId, messageId, createdAtMs)
        _uiState.update { it.copy(readTracking = it.readTracking.copy(myLastReadCreatedAtMs = createdAtMs)) }
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
                if (_uiState.value.currentUserId == event.userId) return@collect
                _uiState.update { it.copy(partner = it.partner.copy(lastReadEvent = event)) }
            }
        }
    }

    private fun loadRecentEmojis() {
        viewModelScope.launch {
            val recents = quickChatEmojiRepository.getRecentEmojis(
                defaultEmojis = CHAT_RECENT_DEFAULT_EMOJIS,
                maxSlots = CHAT_RECENT_MAX_SLOTS,
            )
            _uiState.update { it.copy(recentEmojis = recents) }
        }
    }
}
