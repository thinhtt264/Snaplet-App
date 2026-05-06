package com.thinh.snaplet.ui.screens.spotlight_post

import com.thinh.snaplet.data.model.post.Post
import com.thinh.snaplet.ui.screens.home.PostReactionsUiState

enum class SpotlightPostStatus {
    Forbidden,
    NotFound,
    LoadFailed,
}

data class SpotlightPostUiState(
    val isLoading: Boolean = true,
    val post: Post? = null,
    val status: SpotlightPostStatus? = null,
    val postReactionsState: PostReactionsUiState = PostReactionsUiState.Loading,
    val showReactionsSheet: Boolean = false,
    val quickChatEmojiSlots: List<String> = emptyList(),
)
