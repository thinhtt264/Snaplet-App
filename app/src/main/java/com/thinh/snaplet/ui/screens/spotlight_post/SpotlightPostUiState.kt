package com.thinh.snaplet.ui.screens.spotlight_post

import com.thinh.snaplet.data.model.post.Post
import com.thinh.snaplet.ui.common.UiText
import com.thinh.snaplet.ui.screens.home.PostReactionsUiState

data class SpotlightPostUiState(
    val isLoading: Boolean = true,
    val post: Post? = null,
    val error: UiText? = null,
    val postReactionsState: PostReactionsUiState = PostReactionsUiState.Loading,
    val showReactionsSheet: Boolean = false,
)
