package com.thinh.snaplet.ui.screens.conversation_list

import com.thinh.snaplet.data.model.RelationshipWithUser
import com.thinh.snaplet.data.model.chat.Conversation

data class ConversationListUiState(
    val isLoading: Boolean = true,
    val conversations: List<Conversation> = emptyList(),
    val nextCursor: String? = null,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val friendList: List<RelationshipWithUser> = emptyList(),
    val isFriendListLoading: Boolean = false,
    val friendListError: String? = null,
) {
    val canLoadMore: Boolean get() = nextCursor != null && !isLoadingMore && !isLoading
}
