package com.thinh.snaplet.ui.screens.conversation_list

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Create
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.thinh.snaplet.R
import com.thinh.snaplet.data.model.chat.Conversation
import com.thinh.snaplet.data.model.chat.LastMessage
import com.thinh.snaplet.ui.components.AppIconButton
import com.thinh.snaplet.ui.components.Avatar
import com.thinh.snaplet.ui.components.BaseText
import com.thinh.snaplet.ui.components.IconDecoration
import com.thinh.snaplet.ui.components.IconSpec
import com.thinh.snaplet.ui.theme.Typography
import com.thinh.snaplet.utils.toLocalTimeAgo
import pressScaleClickable

@Composable
fun ConversationListScreen(
    onNavigateBack: () -> Unit,
    onConversationClick: (Conversation) -> Unit = {},
    viewModel: ConversationListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
    ) {
        ConversationHeader(onNavigateBack = onNavigateBack)
        ConversationSearchBar(
            query = searchQuery,
            onQueryChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
        )
        ConversationContent(
            uiState = uiState,
            searchQuery = searchQuery,
            onConversationClick = onConversationClick,
            onRetry = viewModel::loadConversations,
            onLoadMore = viewModel::loadMore,
        )
    }
}

// ─── Header ──────────────────────────────────────────────────────────────────

@Composable
private fun ConversationHeader(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIconButton(
            icon = IconSpec.Vector(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                tint = Color.White,
            ),
            iconSize = 28.dp,
            onClick = onNavigateBack,
            containerColor = Color.Transparent,
            iconDecoration = IconDecoration(padding = 12.dp),
        )
        BaseText(
            text = stringResource(R.string.conversation_list_title),
            typography = Typography.headlineSmall,
            color = Color.White,
            modifier = Modifier.weight(1f),
        )
        AppIconButton(
            icon = IconSpec.Vector(
                imageVector = Icons.Default.MoreVert,
                tint = Color.White,
            ),
            iconSize = 28.dp,
            onClick = {},
            iconDecoration = IconDecoration(padding = 8.dp),
        )
        Spacer(Modifier.width(8.dp))
        AppIconButton(
            icon = IconSpec.Vector(
                imageVector = Icons.Outlined.Create,
                tint = Color.White,
            ),
            iconSize = 28.dp,
            onClick = {},
            iconDecoration = IconDecoration(padding = 8.dp),
        )
        Spacer(Modifier.width(8.dp))
    }
}

// ─── Search bar ──────────────────────────────────────────────────────────────

@Composable
private fun ConversationSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        cursorBrush = SolidColor(cs.primary),
        textStyle = Typography.bodyMedium.copy(cs.onBackground),
        modifier = modifier,
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(CircleShape)
                    .background(cs.surfaceContainer)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = null,
                    tint = cs.onSurface,
                    modifier = Modifier.size(24.dp),
                )
                Box(modifier = Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        BaseText(
                            text = stringResource(R.string.conversation_search_hint),
                            typography = Typography.bodyMedium,
                            color = cs.onSurface,
                        )
                    }
                    innerTextField()
                }
            }
        },
    )
}

// ─── Content ─────────────────────────────────────────────────────────────────

@Composable
private fun ConversationContent(
    uiState: ConversationListUiState,
    searchQuery: String,
    onConversationClick: (Conversation) -> Unit,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        when {
            uiState.isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            uiState.error != null -> {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    BaseText(
                        text = stringResource(R.string.conversation_list_load_failed),
                        color = MaterialTheme.colorScheme.outline,
                        typography = Typography.bodyMedium,
                    )
                    OutlinedButton(onClick = onRetry) {
                        BaseText(
                            text = stringResource(R.string.retry),
                            color = MaterialTheme.colorScheme.onSurface,
                            typography = Typography.labelLarge,
                        )
                    }
                }
            }

            uiState.conversations.isEmpty() -> {
                BaseText(
                    text = stringResource(R.string.conversation_list_empty),
                    color = MaterialTheme.colorScheme.outline,
                    typography = Typography.bodyMedium,
                    modifier = Modifier.align(Alignment.Center),
                )
            }

            else -> {
                val displayList =
                    if (searchQuery.isBlank()) uiState.conversations else uiState.conversations

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .navigationBarsPadding(),
                    contentPadding = PaddingValues(vertical = 8.dp),
                ) {
                    items(displayList, key = { it.id }) { conversation ->
                        ConversationCard(
                            conversation = conversation,
                            onClick = { onConversationClick(conversation) },
                        )
                        HorizontalDivider(
                            color = Color(0xFF161818),
                            thickness = 1.dp,
                            modifier = Modifier.padding(start = 76.dp),
                        )
                    }

                    if (uiState.canLoadMore) {
                        item(key = "load_more") {
                            LaunchedEffect(Unit) { onLoadMore() }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    strokeWidth = 2.dp,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─── Conversation card ────────────────────────────────────────────────────────

@Composable
private fun ConversationCard(
    conversation: Conversation,
    onClick: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val unread = conversation.hasUnread

    val nameColor = if (unread) cs.onBackground else cs.onSurface
    val previewColor = if (unread) cs.onBackground else cs.onSurface
    val metaColor = if (unread) cs.primary else cs.onSurface

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pressScaleClickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(
            avatarUrl = conversation.partner.avatarUrl,
            firstName = conversation.partner.displayName,
            size = 48.dp,
            modifier = Modifier.alpha(if (unread) 1f else 0.7f),
        )

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            BaseText(
                text = conversation.partner.displayName,
                color = nameColor,
                typography = Typography.bodyMedium,
                fontWeight = if (unread) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (conversation.lastMessage != null) {
                BaseText(
                    text = lastMessagePreview(conversation.lastMessage),
                    color = previewColor,
                    fontWeight = if (unread) FontWeight.SemiBold else FontWeight.Normal,
                    typography = Typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Spacer(Modifier.width(8.dp))

        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            conversation.lastMessageAt?.let { time ->
                BaseText(
                    text = time.toLocalTimeAgo(),
                    color = metaColor,
                    typography = Typography.labelSmall,
                    fontWeight = if (unread) FontWeight.Bold else FontWeight.Normal,
                )
            }
            if (unread) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(cs.primary)
                        .align(Alignment.End),
                )
            }
        }
    }
}

@Composable
private fun lastMessagePreview(msg: LastMessage): String {
    return when {
        msg.isDeleted -> stringResource(R.string.conversation_deleted_message)
        msg.type == "image" -> stringResource(R.string.conversation_message_photo)
        else -> msg.content.orEmpty()
    }
}
