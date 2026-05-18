package com.thinh.snaplet.ui.screens.chat

import android.content.ClipData
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.thinh.snaplet.R
import com.thinh.snaplet.data.local.entity.MessageStatus
import com.thinh.snaplet.data.local.entity.toMessage
import com.thinh.snaplet.ui.components.BaseText
import com.thinh.snaplet.ui.components.CappedCountBadge
import com.thinh.snaplet.ui.screens.chat.components.BubblePosition
import com.thinh.snaplet.ui.screens.chat.components.ChatHeader
import com.thinh.snaplet.ui.screens.chat.components.ChatInputBar
import com.thinh.snaplet.ui.screens.chat.components.DateSeparatorItem
import com.thinh.snaplet.ui.screens.chat.components.MessageBubble
import com.thinh.snaplet.ui.screens.chat.components.MessageBubblePlaceholder
import com.thinh.snaplet.ui.screens.chat.components.MessageInspectOverlay
import com.thinh.snaplet.ui.screens.chat.components.MessageReactionsBottomSheet
import com.thinh.snaplet.ui.screens.chat.components.bubbleInspectRoundRect
import com.thinh.snaplet.ui.theme.MotionTokens
import com.thinh.snaplet.ui.theme.Typography
import com.thinh.snaplet.utils.isGreaterWithFallback
import kotlinx.coroutines.launch

private val ChatBg = Color(0xFF0D0D0D)
private val SeparatorColor = Color(0xFF1A1C1C)
private val InspectDimColor = Color.Black.copy(alpha = 0.7f)
private val InspectEdgePadding = 12.dp
private val InspectReactionBarHeight = 60.dp
private val InspectActionMenuHeightMine = 144.dp
private val InspectActionMenuHeightOther = 96.dp

@Composable
fun ChatScreen(
    onNavigateBack: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lazyPagingItems = viewModel.messages.collectAsLazyPagingItems()
    val listState = rememberLazyListState()
    val clipboard = LocalClipboard.current
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val shouldDisablePlacementAnimation =
        WindowInsets.ime.getBottom(density) > 0 &&
                !listState.canScrollBackward &&
                !listState.canScrollForward
    val bubbleBounds = remember { mutableStateMapOf<String, Rect>() }

    var chatAreaHeightPx by remember { mutableIntStateOf(0) }
    var chatAreaWidthPx by remember { mutableIntStateOf(0) }
    var chatAreaTopPx by remember { mutableFloatStateOf(0f) }
    var chatAreaLeftPx by remember { mutableFloatStateOf(0f) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.onResume() }
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { viewModel.onPause() }
    BackHandler(enabled = uiState.inspectedMessage != null) {
        viewModel.dismissInspect()
    }

    LaunchedEffect(listState) {
        var prevNewestLocalId: String? = null
        snapshotFlow {
            val nearBottom = listState.isNearBottomChat()
            val newestLocalId = lazyPagingItems.itemSnapshotList.items
                .firstOrNull { it is ChatListItem.MessageItem }
                ?.let { (it as ChatListItem.MessageItem).message.localId }
            Triple(nearBottom, newestLocalId, listState.firstVisibleItemIndex)
        }.collect { (nearBottom, newestLocalId, firstVisibleIndex) ->
            // auto-scroll only when a truly newer newest-message arrives and user is near bottom.
            if (nearBottom && firstVisibleIndex == 0 && prevNewestLocalId != null && newestLocalId != null && newestLocalId != prevNewestLocalId) {
                listState.scrollToItem(0)
            }
            prevNewestLocalId = newestLocalId
            viewModel.onIsAtBottomChanged(nearBottom)
        }
    }

    val visibleMessages by remember {
        derivedStateOf {
            listState.layoutInfo.visibleItemsInfo.mapNotNull { info ->
                if (info.index < lazyPagingItems.itemCount) {
                    (lazyPagingItems.peek(info.index) as? ChatListItem.MessageItem)?.message
                } else {
                    null
                }
            }
        }
    }
    LaunchedEffect(Unit) {
        snapshotFlow { visibleMessages }.collect { visible ->
            viewModel.onVisibleMessagesChanged(visible.map { it.toMessage() })
        }
    }

    val inspectedMessage = uiState.inspectedMessage
    val inspectMeta by remember(
        inspectedMessage, lazyPagingItems.itemCount, uiState.currentUserId
    ) {
        derivedStateOf {
            val target = inspectedMessage ?: return@derivedStateOf null
            val index = (0 until lazyPagingItems.itemCount).firstOrNull { idx ->
                val item = lazyPagingItems.peek(idx)
                item is ChatListItem.MessageItem && item.message.localId == target.localId
            } ?: return@derivedStateOf null
            val prevSenderSame =
                lazyPagingItems.adjacentMessageSenderSame(index, direction = 1, target.senderId)
            val nextSenderSame =
                lazyPagingItems.adjacentMessageSenderSame(index, direction = -1, target.senderId)
            val position = bubbleChainPosition(prevSenderSame, nextSenderSame)
            val isMine = target.senderId == uiState.currentUserId
            InspectMeta(
                isMine = isMine,
                position = position,
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ChatBg)
            .pointerInput(Unit) { detectTapGestures(onTap = { focusManager.clearFocus() }) },
    ) {
        ChatHeader(
            name = viewModel.partnerName,
            avatarUrl = viewModel.partnerAvatarUrl,
            isOnline = uiState.isPartnerOnline,
            isPartnerTyping = uiState.partner.isTyping,
            onNavigateBack = onNavigateBack,
            onMore = { /* TODO */ },
        )

        HorizontalDivider(color = SeparatorColor, thickness = 1.dp)

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .onGloballyPositioned { coords ->
                    chatAreaHeightPx = coords.size.height
                    chatAreaWidthPx = coords.size.width
                    chatAreaTopPx = coords.positionInRoot().y
                    chatAreaLeftPx = coords.positionInRoot().x
                }) {
            val refreshError = lazyPagingItems.loadState.refresh as? LoadState.Error
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
                        OutlinedButton(onClick = { viewModel.onSendMessage(uiState.draftMessage) }) {
                            BaseText(
                                text = stringResource(R.string.retry),
                                color = Color.White,
                                typography = Typography.labelLarge,
                            )
                        }
                    }
                }

                refreshError != null && lazyPagingItems.itemCount == 0 -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        BaseText(
                            text = refreshError.error.localizedMessage
                                ?: stringResource(R.string.error),
                            color = Color.White.copy(alpha = 0.6f),
                            typography = Typography.bodyMedium,
                        )
                        OutlinedButton(onClick = { lazyPagingItems.retry() }) {
                            BaseText(
                                text = stringResource(R.string.retry),
                                color = Color.White,
                                typography = Typography.labelLarge,
                            )
                        }
                    }
                }

                else -> {
                    /** True while user scrolls toward newest (down); reset at bottom or when scrolling back into history. */
                    var scrollDownTowardLatest by remember { mutableStateOf(false) }
                    var prevScrollDepth by remember { mutableStateOf<Long?>(null) }
                    LaunchedEffect(listState) {
                        snapshotFlow {
                            Triple(
                                listState.isNearBottomChat(),
                                listState.firstVisibleItemIndex,
                                listState.firstVisibleItemScrollOffset,
                            )
                        }.collect { (nearBottom, idx, off) ->
                            val depth = idx.toLong() * 1_000_000L + off
                            if (nearBottom) {
                                scrollDownTowardLatest = false
                                prevScrollDepth = depth
                                return@collect
                            }
                            val prev = prevScrollDepth
                            if (prev != null) {
                                val delta = depth - prev
                                when {
                                    delta < 0 -> scrollDownTowardLatest = true
                                    delta > 0 -> scrollDownTowardLatest = false
                                }
                            }
                            prevScrollDepth = depth
                        }
                    }
                    val showScrollDownFab by remember {
                        derivedStateOf {
                            if (listState.isNearBottomChat()) return@derivedStateOf false
                            val unread = uiState.readTracking.incomingUnread.count
                            unread > 0 || scrollDownTowardLatest
                        }
                    }
                    Box(modifier = Modifier.fillMaxSize()) {
                        LazyColumn(
                            state = listState,
                            reverseLayout = true,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            items(
                                count = lazyPagingItems.itemCount,
                                key = { index ->
                                    when (val item = lazyPagingItems.peek(index)) {
                                        is ChatListItem.MessageItem -> item.message.localId
                                        is ChatListItem.DateSeparator -> "separator_${item.dateMillis}"
                                        null -> "placeholder_$index"
                                    }
                                },
                            ) { index ->
                                when (val item = lazyPagingItems[index]) {
                                    is ChatListItem.MessageItem -> {
                                        val message = item.message
                                        val isMine = message.senderId == uiState.currentUserId
                                        val isPending = message.status == MessageStatus.PENDING
                                        val isError = message.status == MessageStatus.FAILED

                                        val prevSenderSame =
                                            lazyPagingItems.adjacentMessageSenderSame(
                                                index = index,
                                                direction = 1,
                                                senderId = message.senderId,
                                            )
                                        val nextSenderSame =
                                            lazyPagingItems.adjacentMessageSenderSame(
                                                index = index,
                                                direction = -1,
                                                senderId = message.senderId,
                                            )
                                        val position =
                                            bubbleChainPosition(prevSenderSame, nextSenderSame)

                                        val partnerReadHorizon = uiState.partner.readHorizon
                                        val isPartnerSeen = isMine && isGreaterWithFallback(
                                            partnerReadHorizon, message.createdAt, false
                                        )

                                        var appeared by remember(message.localId) {
                                            mutableStateOf(
                                                false
                                            )
                                        }
                                        val isFreshItem = !appeared

                                        val scale by animateFloatAsState(
                                            targetValue = if (appeared) 1f else 0.9f,
                                            animationSpec = tween(
                                                if (isFreshItem) MotionTokens.Slow else MotionTokens.Fast,
                                                easing = FastOutSlowInEasing
                                            ),
                                            label = "bubble_scale",
                                        )
                                        val alpha by animateFloatAsState(
                                            targetValue = if (appeared) 1f else 0f,
                                            animationSpec = tween(
                                                if (isFreshItem) MotionTokens.Emphasized else MotionTokens.Fast,
                                                easing = FastOutSlowInEasing
                                            ),
                                            label = "bubble_alpha",
                                        )

                                        SideEffect { appeared = true }

                                        val bubbleModifier = if (shouldDisablePlacementAnimation) {
                                            Modifier
                                        } else {
                                            Modifier.animateItem(
                                                fadeInSpec = null,
                                                placementSpec = tween(
                                                    MotionTokens.Slow,
                                                    easing = FastOutSlowInEasing,
                                                ),
                                                fadeOutSpec = tween(
                                                    MotionTokens.Emphasized,
                                                    easing = FastOutSlowInEasing
                                                ),
                                            )
                                        }

                                        MessageBubble(
                                            modifier = bubbleModifier
                                                .graphicsLayer {
                                                    this.alpha = alpha
                                                    this.scaleX = scale
                                                    this.scaleY = scale
                                                    transformOrigin = if (isMine) TransformOrigin(
                                                        1f, 1f
                                                    ) else TransformOrigin(0f, 1f)
                                                },
                                            message = message,
                                            isMine = isMine,
                                            isPending = isPending,
                                            isError = isError,
                                            showSeenTick = isPartnerSeen,
                                            position = position,
                                            onClick = viewModel::onMessageLongPress,
                                            onReactionDockClick = viewModel::onMessageReactionDockClick,
                                            onBoundsChanged = { key, rect ->
                                                bubbleBounds[key] = rect
                                            },
                                        )
                                    }

                                    is ChatListItem.DateSeparator -> DateSeparatorItem(item.dateMillis)

                                    null -> MessageBubblePlaceholder()
                                }
                            }

                            if (lazyPagingItems.loadState.append is LoadState.Loading && listState.firstVisibleItemIndex > 0) {
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

                        androidx.compose.animation.AnimatedVisibility(
                            visible = showScrollDownFab,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(end = 12.dp, bottom = 8.dp),
                            enter = fadeIn(animationSpec = tween(MotionTokens.Normal)),
                            exit = fadeOut(animationSpec = tween(MotionTokens.Fast)),
                        ) {
                            ChatScrollToBottomFab(
                                unreadCount = uiState.readTracking.incomingUnread.count,
                                onClick = {
                                    coroutineScope.launch {
                                        listState.animateScrollToItem(0)
                                    }
                                },
                                scrollToBottomLabel = stringResource(R.string.chat_scroll_to_bottom_cd),
                            )
                        }
                    }
                }
            }

            androidx.compose.animation.AnimatedVisibility(
                visible = inspectedMessage != null && inspectMeta != null,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                val message = inspectedMessage ?: return@AnimatedVisibility
                val meta = inspectMeta ?: return@AnimatedVisibility

                val anchorRoot = bubbleBounds[message.localId]
                val layout = inspectOverlayLayout(
                    anchorRoot = anchorRoot,
                    chatLeftPx = chatAreaLeftPx,
                    chatTopPx = chatAreaTopPx,
                    chatWidthPx = chatAreaWidthPx,
                    chatHeightPx = chatAreaHeightPx,
                    density = density,
                    meta = meta,
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(
                            message.localId,
                            anchorRoot,
                            chatAreaLeftPx,
                            chatAreaTopPx,
                        ) {
                            detectTapGestures { offset ->
                                val bubbleChat = anchorRoot?.let {
                                    bubbleRectRootToChat(it, chatAreaLeftPx, chatAreaTopPx)
                                }
                                if (bubbleChat != null && bubbleChat.contains(offset)) return@detectTapGestures
                                viewModel.dismissInspect()
                            }
                        },
                ) {
                    InspectDimScrim(
                        anchorRoot = anchorRoot,
                        chatLeftPx = chatAreaLeftPx,
                        chatTopPx = chatAreaTopPx,
                        density = density,
                        meta = meta,
                    )
                    MessageInspectOverlay(
                        message = message,
                        isMine = meta.isMine,
                        panelTop = layout.panelTopDp,
                        bubbleCenterXDp = layout.bubbleCenterXDp,
                        chatAreaWidthDp = layout.chatAreaWidthDp,
                        maxHeight = layout.panelMaxHeightDp,
                        isFlipped = layout.flipPanelVertical,
                        recentEmojis = uiState.recentEmojis,
                        onEmojiClick = { emoji ->
                            viewModel.reactToMessage(
                                messageId = message.id,
                                emoji = emoji,
                            )
                        },
                        onCopy = { copiedMessage ->
                            val messageText = copiedMessage.text?.trim().orEmpty()
                            if (messageText.isNotEmpty()) {
                                coroutineScope.launch {
                                    clipboard.setClipEntry(
                                        ClipEntry(
                                            ClipData.newPlainText("message", messageText)
                                        )
                                    )
                                }
                            }
                        },
                        onDismiss = viewModel::dismissInspect,
                    )
                }
            }
        }

        ChatInputBar(
            value = uiState.draftMessage.orEmpty(),
            onValueChange = viewModel::onChangeDraftMessage,
            onSendMessage = { text ->
                viewModel.onSendMessage(text)
                coroutineScope.launch { listState.animateScrollToItem(0) }
            },
            onAttach = { /* TODO */ },
        )
    }

    if (uiState.messageReactionsSheet.isVisible) {
        MessageReactionsBottomSheet(
            reactions = uiState.messageReactionsSheet.reactions,
            isLoading = uiState.messageReactionsSheet.isLoading,
            error = uiState.messageReactionsSheet.error,
            currentUserId = uiState.currentUserId,
            onDismiss = viewModel::dismissMessageReactionsSheet,
            onMyReactionClick = viewModel::onMyMessageReactionClick,
        )
    }
}

private fun LazyPagingItems<ChatListItem>.adjacentMessageSenderSame(
    index: Int,
    direction: Int,
    senderId: String,
): Boolean {
    var cursor = index + direction
    while (cursor in 0 until itemCount) {
        when (val item = peek(cursor)) {
            is ChatListItem.MessageItem -> return item.message.senderId == senderId
            is ChatListItem.DateSeparator -> cursor += direction
            null -> return false
        }
    }
    return false
}

private fun LazyListState.isNearBottomChat(): Boolean {
    val idx = firstVisibleItemIndex
    val offset = firstVisibleItemScrollOffset
    val viewport = layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset
    return idx == 0 && (viewport == 0 || offset <= viewport * 0.3f)
}

@Composable
private fun ChatScrollToBottomFab(
    unreadCount: Int,
    onClick: () -> Unit,
    scrollToBottomLabel: String,
) {
    Box(modifier = Modifier.wrapContentSize()) {
        FloatingActionButton(
            onClick = onClick,
            modifier = Modifier.size(44.dp),
            shape = CircleShape,
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onBackground,
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = 6.dp,
                pressedElevation = 8.dp,
            ),
        ) {
            Icon(
                imageVector = Icons.Outlined.KeyboardArrowDown,
                contentDescription = scrollToBottomLabel,
                modifier = Modifier.size(28.dp)
            )
        }
        if (unreadCount > 0) {
            CappedCountBadge(
                count = unreadCount,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-12).dp),
                backgroundColor = MaterialTheme.colorScheme.surfaceTint,
                contentColor = Color.Black,
                shape = CircleShape,
                typography = Typography.labelSmall,
                contentPadding = PaddingValues(horizontal = 5.dp, vertical = 2.dp),
                minSize = 20.dp,
            )
        }
    }
}

private fun bubbleChainPosition(prevSame: Boolean, nextSame: Boolean): BubblePosition = when {
    prevSame && nextSame -> BubblePosition.MIDDLE
    prevSame && !nextSame -> BubblePosition.LAST
    !prevSame && nextSame -> BubblePosition.FIRST
    else -> BubblePosition.SINGLE
}

private fun bubbleRectRootToChat(root: Rect, chatLeft: Float, chatTop: Float): Rect = Rect(
    root.left - chatLeft,
    root.top - chatTop,
    root.right - chatLeft,
    root.bottom - chatTop,
)

private data class InspectOverlayLayout(
    val chatAreaWidthDp: Dp,
    val bubbleCenterXDp: Dp,
    val panelTopDp: Dp,
    val panelMaxHeightDp: Dp,
    val flipPanelVertical: Boolean,
)

private fun inspectOverlayLayout(
    anchorRoot: Rect?,
    chatLeftPx: Float,
    chatTopPx: Float,
    chatWidthPx: Int,
    chatHeightPx: Int,
    density: Density,
    meta: InspectMeta,
): InspectOverlayLayout {
    val chatH = with(density) { chatHeightPx.toDp() }.coerceAtLeast(200.dp)
    val chatW = with(density) { chatWidthPx.toDp() }.coerceAtLeast(1.dp)
    val topDp = with(density) { ((anchorRoot?.top ?: 0f) - chatTopPx).toDp() }
    val bottomDp = with(density) { ((anchorRoot?.bottom ?: 0f) - chatTopPx).toDp() }
    val centerXPx = if (anchorRoot != null) {
        (anchorRoot.left + anchorRoot.right) / 2f - chatLeftPx
    } else {
        chatWidthPx / 2f
    }
    val bubbleCenterXDp = with(density) { centerXPx.toDp() }.coerceIn(
        InspectEdgePadding,
        (chatW - InspectEdgePadding).coerceAtLeast(InspectEdgePadding),
    )
    val actionH = if (meta.isMine) InspectActionMenuHeightMine else InspectActionMenuHeightOther
    val panelH = InspectReactionBarHeight + actionH
    val midY = (topDp + bottomDp) / 2f
    val edge = InspectEdgePadding
    val panelTopNormal = (midY - panelH / 2f).coerceIn(
        edge,
        (chatH - panelH - edge).coerceAtLeast(edge),
    )
    val flip = (topDp - edge) < InspectReactionBarHeight
    val panelTop = if (flip) {
        (midY - actionH / 2f).coerceIn(
            edge,
            (chatH - panelH - edge).coerceAtLeast(edge),
        )
    } else {
        panelTopNormal
    }
    return InspectOverlayLayout(
        chatAreaWidthDp = chatW,
        bubbleCenterXDp = bubbleCenterXDp,
        panelTopDp = panelTop,
        panelMaxHeightDp = (chatH - panelTop - edge).coerceAtLeast(120.dp),
        flipPanelVertical = flip,
    )
}

@Composable
private fun InspectDimScrim(
    anchorRoot: Rect?,
    chatLeftPx: Float,
    chatTopPx: Float,
    density: Density,
    meta: InspectMeta,
) {
    Canvas(Modifier.fillMaxSize()) {
        val ar = anchorRoot
        if (ar != null) {
            val hole = bubbleInspectRoundRect(
                left = ar.left - chatLeftPx,
                top = ar.top - chatTopPx,
                right = ar.right - chatLeftPx,
                bottom = ar.bottom - chatTopPx,
                density = density,
                isMine = meta.isMine,
                position = meta.position,
            )
            val dimPath = Path().apply {
                addRect(Rect(0f, 0f, size.width, size.height))
                addRoundRect(hole)
                fillType = PathFillType.EvenOdd
            }
            drawPath(dimPath, color = InspectDimColor)
            val outline = Path().apply { addRoundRect(hole) }
            drawPath(
                path = outline,
                color = Color.White.copy(alpha = 0.14f),
                style = Stroke(width = 1.dp.toPx()),
            )
        } else {
            drawRect(color = InspectDimColor)
        }
    }
}

private data class InspectMeta(
    val isMine: Boolean,
    val position: BubblePosition,
)