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
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

private const val OUT_GOING_TYPING_TIMEOUT_MS = 1_500L
private const val IN_COMING_TYPING_TIMEOUT_MS = 3_000L
private const val MARK_READ_DEBOUNCE_MS = 500L
private const val RESUME_LOAD_THROTTLE_MS = 1_500L
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
            partner = PartnerState(lastReadAtFallback = route.partnerLastReadAtMs?.let(::Date)),
            readTracking = ReadTrackingState(myLastReadCreatedAt = route.myLastReadAtMs?.let(::Date)),
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
    private val resumeLoadThrottler = Throttler(RESUME_LOAD_THROTTLE_MS)

    init {
        loadCurrentUser()
        observeIncomingTypingEvents()
        observeIncomingReadReceipts()
        observeNetworkReconnect()
        loadRecentEmojis()
        loadMessages()
    }

    override fun onCleared() {
        super.onCleared()
        chatRepository.disconnectChatSocket()
    }

    fun onResume() {
        resumeLoadThrottler.run { loadMessages() }
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

    fun reactToMessage(messageId: String, emoji: String) {
        val selectedEmoji = emoji.trim()
        if (selectedEmoji.isEmpty()) return
        val safeMessageId = messageId.trim()
        if (safeMessageId.isEmpty()) return

        viewModelScope.launch {
            chatRepository.reactToMessage(messageId = safeMessageId, emoji = selectedEmoji)
                .onSuccess {
                    onRecentEmojiUsed(selectedEmoji)
                }
                .onFailure { error ->
                    Logger.e("reactToMessage failed: ${error.message}")
                }
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

    private fun loadMessages() {
        viewModelScope.launch {
            if (conversationId.isEmpty()) {
                val recipientId = route.recipientId ?: return@launch
                val resolvedConversationId = resolveConversationId(recipientId)
                if (resolvedConversationId.isNullOrEmpty()) return@launch
                conversationId = resolvedConversationId
                _activeConversationId.value = resolvedConversationId
            }
            connectSocketAndSyncMessages()
        }
    }

    private suspend fun resolveConversationId(recipientId: String): String? {
        _uiState.update { it.copy(messageList = it.messageList.copy(isLoading = true, error = null)) }
        var resolvedConversationId: String? = null
        chatRepository.lookupConversationId(recipientId)
            .onSuccess { lookedUpConversationId ->
                resolvedConversationId = lookedUpConversationId
                _uiState.update { it.copy(messageList = it.messageList.copy(isLoading = false)) }
            }
            .onFailure { error ->
                Logger.e("lookupConversationId failed: ${error.message}")
                _uiState.update { it.copy(messageList = it.messageList.copy(isLoading = false)) }
            }
        return resolvedConversationId
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
            triggerMarkSeen(newestMessageId, Date())
        }
        _uiState.update { it.copy(readTracking = it.readTracking.copy(incomingUnread = IncomingUnreadState())) }
    }

    fun onVisibleMessagesChanged(visibleMessages: List<Message>) {
        markReadJob?.cancel()
        markReadJob = viewModelScope.launch {
            delay(MARK_READ_DEBOUNCE_MS)
            val newest = visibleMessages
                .filter { it.status == null || it.status == MessageStatus.SENT }
                .maxByOrNull { it.createdAt } ?: return@launch
            val myLastRead = _uiState.value.readTracking.myLastReadCreatedAt
            if (myLastRead == null || newest.createdAt.after(myLastRead)) {
                triggerMarkSeen(newest.id, newest.createdAt)
            }
        }
    }

    private fun triggerMarkSeen(messageId: String, createdAt: Date) {
        markMessageSeenUseCase(conversationId, messageId, createdAt)
        _uiState.update { it.copy(readTracking = it.readTracking.copy(myLastReadCreatedAt = createdAt)) }
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
