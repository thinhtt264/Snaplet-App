package com.thinh.snaplet.ui.screens.spotlight_post

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.thinh.snaplet.R
import com.thinh.snaplet.data.repository.post.PostRepository
import com.thinh.snaplet.domain.post.MapPostReactionUsersUseCase
import com.thinh.snaplet.navigation.SpotlightPost
import com.thinh.snaplet.ui.common.UiText
import com.thinh.snaplet.ui.screens.home.PostReactionsUiState
import com.thinh.snaplet.utils.Logger
import com.thinh.snaplet.utils.network.onFailure
import com.thinh.snaplet.utils.network.onSuccess
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SpotlightPostViewModel @Inject constructor(
    private val postRepository: PostRepository,
    private val mapPostReactionUsersUseCase: MapPostReactionUsersUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val route = savedStateHandle.toRoute<SpotlightPost>()

    private val _uiState = MutableStateFlow(SpotlightPostUiState())
    val uiState: StateFlow<SpotlightPostUiState> = _uiState.asStateFlow()

    private var postReactionsJob: Job? = null

    init {
        loadPost()
    }

    fun loadPost() {
        viewModelScope.launch {
            postReactionsJob?.cancel()
            _uiState.update {
                it.copy(
                    isLoading = true,
                    error = null,
                    postReactionsState = PostReactionsUiState.Loading,
                )
            }

            postRepository.getPostById(route.postId).fold(
                onSuccess = { post ->
                    _uiState.update {
                        it.copy(isLoading = false, post = post, error = null)
                    }
                    if (post.isOwnPost) {
                        loadPostReactions(postId = post.id, isOwnerViewed = post.isOwnerViewedPost)
                    } else {
                        _uiState.update {
                            it.copy(postReactionsState = PostReactionsUiState.Result(emptyList()))
                        }
                    }
                },
                onFailure = {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = UiText.StringResource(R.string.error_load_post),
                        )
                    }
                },
            )
        }
    }

    fun onPostActivityClick() {
        val state = _uiState.value.postReactionsState
        if (state is PostReactionsUiState.Result && state.reactions.isNotEmpty()) {
            _uiState.update { it.copy(showReactionsSheet = true) }
        }
    }

    fun onReactionsSheetDismissed() {
        _uiState.update { it.copy(showReactionsSheet = false) }
    }

    private fun loadPostReactions(postId: String, isOwnerViewed: Boolean) {
        postReactionsJob?.cancel()
        _uiState.update { it.copy(postReactionsState = PostReactionsUiState.Loading) }
        postReactionsJob = viewModelScope.launch {
            postRepository.getPostReactions(postId).fold(
                onSuccess = { reactions ->
                    val mapped = runCatching { mapPostReactionUsersUseCase(reactions) }
                        .onFailure { error ->
                            Logger.e(
                                "Failed to map post reactions for spotlight postId=$postId: ${error.message}"
                            )
                        }
                        .getOrDefault(emptyList())
                    _uiState.update {
                        it.copy(postReactionsState = PostReactionsUiState.Result(mapped))
                    }
                    if (!isOwnerViewed) {
                        markPostOwnerViewed(postId)
                    }
                },
                onFailure = {
                    _uiState.update {
                        it.copy(postReactionsState = PostReactionsUiState.Result(emptyList()))
                    }
                },
            )
        }
    }

    private fun markPostOwnerViewed(postId: String) {
        viewModelScope.launch {
            postRepository.markPostOwnerViewed(postId)
                .onSuccess {
                    postRepository.getPostById(postId)
                        .onSuccess { latestPost ->
                            if (!latestPost.isOwnerViewedPost) return@onSuccess
                            _uiState.update { state ->
                                state.copy(post = latestPost)
                            }
                        }
                        .onFailure { error ->
                            Logger.e(
                                "Failed to refresh spotlight post after owner viewed: ${error.message}"
                            )
                        }
                }
                .onFailure { error ->
                    Logger.e("Failed to mark spotlight post owner viewed: ${error.message}")
                }
        }
    }
}
