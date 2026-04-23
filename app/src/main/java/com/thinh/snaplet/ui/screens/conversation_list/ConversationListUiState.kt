package com.thinh.snaplet.ui.screens.conversation_list

import com.thinh.snaplet.data.model.RelationshipWithUser
import com.thinh.snaplet.data.model.user.UserProfile

data class ConversationUiModel(
    val id: String,
    val participantName: String,
    val participantAvatarUrl: String?,
    val lastMessageText: String?,
    val lastMessageType: String?,
    val isLastMessageDeleted: Boolean,
    val lastMessageAt: Long?,
    val myLastSeenAt: Long?,
    val partnerLastSeenAt: Long?,
    val hasUnread: Boolean,
    val isLastMessageMine: Boolean,
    val partnerHasSeen: Boolean,
)

data class ConversationListUiState(
    val isLoading: Boolean = true,
    val conversations: List<ConversationUiModel> = emptyList(),
    val nextCursor: String? = null,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val friendList: List<RelationshipWithUser> = emptyList(),
    val isFriendListLoading: Boolean = false,
    val friendListError: String? = null,
    val userProfile: UserProfile? = null,
) {
    val canLoadMore: Boolean get() = nextCursor != null && !isLoadingMore && !isLoading
}
