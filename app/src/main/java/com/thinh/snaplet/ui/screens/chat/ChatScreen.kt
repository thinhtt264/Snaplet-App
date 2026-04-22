package com.thinh.snaplet.ui.screens.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.thinh.snaplet.R
import com.thinh.snaplet.ui.components.BaseText
import com.thinh.snaplet.ui.screens.chat.components.BubblePosition
import com.thinh.snaplet.ui.screens.chat.components.ChatHeader
import com.thinh.snaplet.ui.screens.chat.components.ChatInputBar
import com.thinh.snaplet.ui.screens.chat.components.MessageBubble
import com.thinh.snaplet.ui.screens.chat.components.TypingIndicator
import com.thinh.snaplet.ui.theme.Typography
import kotlinx.coroutines.launch

// ─── Design tokens ────────────────────────────────────────────────────────────

private val ChatBg = Color(0xFF0D0D0D)
private val SeparatorColor = Color(0xFF1A1C1C)

// ─── Screen ───────────────────────────────────────────────────────────────────

@Composable
fun ChatScreen(
    onNavigateBack: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val focusManager = LocalFocusManager.current

    val coroutineScope = rememberCoroutineScope()

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.onResume() }
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { viewModel.onPause() }

    val shouldLoadMore by remember {
        derivedStateOf {
            val totalItems = listState.layoutInfo.totalItemsCount
            val lastVisibleItemIndex =
                listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItems > 0 && lastVisibleItemIndex >= (totalItems * 0.8f).toInt()
        }
    }

    LaunchedEffect(shouldLoadMore, uiState.messageList.canLoadMore) {
        if (shouldLoadMore && uiState.messageList.canLoadMore) {
            viewModel.loadMore()
        }
    }

    // derivedStateOf makes messageCount a Compose State so snapshotFlow can track it.
    // This is key: snapshotFlow reads count + scroll position in the same snapshot frame,
    val messageCountState = remember { derivedStateOf { uiState.messageList.messages.size } }
    LaunchedEffect(listState) {
        var prevMessageCount = 0
        snapshotFlow {
            val count = messageCountState.value
            val idx = listState.firstVisibleItemIndex
            val offset = listState.firstVisibleItemScrollOffset
            val viewport =
                listState.layoutInfo.viewportEndOffset - listState.layoutInfo.viewportStartOffset
            count to (idx == 0 && (viewport == 0 || offset <= viewport * 0.25f))
        }.collect { (count, nearBottom) ->
            if (count > prevMessageCount && nearBottom) {
                listState.scrollToItem(0)
            }
            prevMessageCount = count
            viewModel.onIsAtBottomChanged(nearBottom)
        }
    }

    // Track visible messages for mark-seen; debounce is handled in the ViewModel.
    val visibleMessages by remember {
        derivedStateOf {
            listState.layoutInfo.visibleItemsInfo
                .mapNotNull { info -> uiState.messageList.messages.getOrNull(info.index) }
        }
    }
    LaunchedEffect(Unit) {
        snapshotFlow { visibleMessages }
            .collect { visible -> viewModel.onVisibleMessagesChanged(visible) }
    }
    // TODO: NewMessagesBanner UI — logic is ready via uiState.readTracking.incomingUnread

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ChatBg)
            .pointerInput(Unit) { detectTapGestures(onTap = { focusManager.clearFocus() }) },
    ) {
        ChatHeader(
            name = viewModel.partnerName,
            avatarUrl = viewModel.partnerAvatarUrl,
            isOnline = true,
            onNavigateBack = onNavigateBack,
            onMore = { /* TODO */ },
        )

        HorizontalDivider(color = SeparatorColor, thickness = 1.dp)

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when {
                uiState.messageList.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                uiState.messageList.error != null -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        BaseText(
                            text = uiState.messageList.error ?: "",
                            color = Color.White.copy(alpha = 0.6f),
                            typography = Typography.bodyMedium,
                        )
                        OutlinedButton(onClick = viewModel::loadMessages) {
                            BaseText(
                                text = stringResource(R.string.retry),
                                color = Color.White,
                                typography = Typography.labelLarge,
                            )
                        }
                    }
                }

                else -> {
                    val partnerReadHorizonMs = uiState.partner.readHorizonMs
                    val lastSentByMeId =
                        remember(uiState.messageList.messages, uiState.currentUserId) {
                            uiState.messageList.messages.firstOrNull { it.senderId == uiState.currentUserId }?.id
                        }
                    val lastReadByPartnerMessageId =
                        remember(
                            uiState.messageList.messages,
                            uiState.currentUserId,
                            partnerReadHorizonMs
                        ) {
                            if (partnerReadHorizonMs == null) null
                            else uiState.messageList.messages.firstOrNull {
                                it.senderId == uiState.currentUserId &&
                                        it.createdAt.time <= partnerReadHorizonMs
                            }?.id
                        }

                    LazyColumn(
                        state = listState,
                        reverseLayout = true,
                        modifier = Modifier
                            .fillMaxSize()
                            .animateContentSize(),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        itemsIndexed(
                            items = uiState.messageList.messages,
                            key = { _, item -> item.clientUuid },
                        ) { index, message ->
                            val isMine = message.senderId == uiState.currentUserId
                            val isPending = message.clientUuid in uiState.pendingClientUuids
                            val isError = message.clientUuid in uiState.errorClientUuids
                            val showTick =
                                isMine && message.id == lastSentByMeId && message.id != lastReadByPartnerMessageId

                            // reverseLayout=true: index+1 = older (visually above),
                            //                     index-1 = newer (visually below)
                            val prevSenderSame =
                                uiState.messageList.messages.getOrNull(index + 1)?.senderId == message.senderId
                            val nextSenderSame =
                                uiState.messageList.messages.getOrNull(index - 1)?.senderId == message.senderId
                            val position = when {
                                prevSenderSame && nextSenderSame -> BubblePosition.MIDDLE
                                prevSenderSame && !nextSenderSame -> BubblePosition.LAST
                                !prevSenderSame && nextSenderSame -> BubblePosition.FIRST
                                else -> BubblePosition.SINGLE
                            }

                            MessageBubble(
                                message = message,
                                isMine = isMine,
                                isPending = isPending,
                                isError = isError,
                                showTick = showTick,
                                showSeenTick = isMine && message.id == lastReadByPartnerMessageId,
                                position = position,
                            )
                        }

                        // Older-messages loading spinner at the visual top
                        if (uiState.messageList.isLoadingMore) {
                            item(key = "loading_more") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Typing indicator sits between the message list and the input bar
        AnimatedVisibility(
            visible = uiState.partner.isTyping,
            enter = fadeIn(),
            exit = ExitTransition.None,
        ) {
            Row(
                modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 4.dp),
            ) {
                TypingIndicator()
            }
        }

        ChatInputBar(
            value = uiState.draftMessage.orEmpty(),
            onValueChange = viewModel::onChangeDraftMessage,
            onSendMessage = { text ->
                viewModel.onSendMessage(text)
                coroutineScope.launch { listState.scrollToItem(0) }
            },
            onAttach = { /* TODO */ },
        )
    }
}