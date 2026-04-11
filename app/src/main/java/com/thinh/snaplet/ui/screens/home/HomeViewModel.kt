package com.thinh.snaplet.ui.screens.home

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thinh.snaplet.R
import com.thinh.snaplet.data.model.RelationshipCounts
import com.thinh.snaplet.data.model.RelationshipStatus
import com.thinh.snaplet.data.model.RelationshipWithUser
import com.thinh.snaplet.data.model.media.ImageTransform
import com.thinh.snaplet.data.model.post.NewPostUpdate
import com.thinh.snaplet.data.model.post.Post
import com.thinh.snaplet.data.repository.MediaRepository
import com.thinh.snaplet.data.repository.UserRepository
import com.thinh.snaplet.data.repository.post.PostRepository
import com.thinh.snaplet.domain.feed.FetchNewerFeedUseCase
import com.thinh.snaplet.domain.feed.GetNewsfeedUseCase
import com.thinh.snaplet.domain.feed.GetNewsfeedUseCase.Companion.FEED_PAGE_LIMIT
import com.thinh.snaplet.domain.feed.GetNewsfeedUseCase.Companion.GRID_LOAD_MORE_STEP
import com.thinh.snaplet.domain.feed.ObserveNewPostEventUseCase
import com.thinh.snaplet.domain.feed.ShouldMarkLatestPostAsSeenUseCase
import com.thinh.snaplet.domain.feed.ShouldTriggerLoadMoreUseCase
import com.thinh.snaplet.domain.media.ValidateCaptureReadinessUseCase
import com.thinh.snaplet.domain.model.CaptureReadiness
import com.thinh.snaplet.domain.model.FloatDirection
import com.thinh.snaplet.domain.model.NewerFeedResult
import com.thinh.snaplet.domain.model.PostAction
import com.thinh.snaplet.domain.model.RelationshipAction
import com.thinh.snaplet.domain.model.UploadPostResult
import com.thinh.snaplet.domain.post.BuildPostShareContentUseCase
import com.thinh.snaplet.domain.post.CreateTempPostUseCase
import com.thinh.snaplet.domain.post.DeletePostUseCase
import com.thinh.snaplet.domain.post.PostCreateAudience
import com.thinh.snaplet.domain.post.GetAvailablePostActionsUseCase
import com.thinh.snaplet.domain.post.MapPostReactionUsersUseCase
import com.thinh.snaplet.domain.post.UploadPostUseCase
import com.thinh.snaplet.domain.post.ValidateRetryUploadUseCase
import com.thinh.snaplet.domain.post.ValidateUploadPostUseCase
import com.thinh.snaplet.domain.relationship.AcceptFriendRequestUseCase
import com.thinh.snaplet.domain.relationship.FormatFriendSearchResultsUseCase
import com.thinh.snaplet.domain.relationship.GetRelationshipActionUseCase
import com.thinh.snaplet.domain.relationship.GetRelationshipsByStatusesUseCase
import com.thinh.snaplet.domain.relationship.RemoveFriendUseCase
import com.thinh.snaplet.domain.relationship.RemoveRelationshipUseCase
import com.thinh.snaplet.platform.network.ConnectivityObserver
import com.thinh.snaplet.platform.permission.Permission
import com.thinh.snaplet.platform.permission.PermissionManager
import com.thinh.snaplet.platform.share.ShareApp
import com.thinh.snaplet.platform.share.ShareManager
import com.thinh.snaplet.platform.widget.WidgetUpdateManager
import com.thinh.snaplet.ui.common.UiText
import com.thinh.snaplet.data.model.post.PostAudience
import com.thinh.snaplet.ui.components.EmojiFloatController
import com.thinh.snaplet.ui.overlay.OverlayEventBus
import com.thinh.snaplet.ui.overlay.SheetOption
import com.thinh.snaplet.ui.theme.Error50
import com.thinh.snaplet.utils.CrashlyticsLogger
import com.thinh.snaplet.utils.FileUtils
import com.thinh.snaplet.utils.Logger
import com.thinh.snaplet.utils.network.ApiErrorCode
import com.thinh.snaplet.utils.network.onFailure
import com.thinh.snaplet.utils.network.onSuccess
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlin.math.min

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val permissionManager: PermissionManager,
    private val getNewsfeedUseCase: GetNewsfeedUseCase,
    private val fetchNewerFeedUseCase: FetchNewerFeedUseCase,
    private val shouldTriggerLoadMoreUseCase: ShouldTriggerLoadMoreUseCase,
    private val validateCaptureReadinessUseCase: ValidateCaptureReadinessUseCase,
    private val createTempPostUseCase: CreateTempPostUseCase,
    private val buildPostShareContentUseCase: BuildPostShareContentUseCase,
    private val validateUploadPostUseCase: ValidateUploadPostUseCase,
    private val uploadPostUseCase: UploadPostUseCase,
    private val validateRetryUploadUseCase: ValidateRetryUploadUseCase,
    private val getAvailablePostActionsUseCase: GetAvailablePostActionsUseCase,
    private val deletePostUseCase: DeletePostUseCase,
    private val getRelationshipsByStatusesUseCase: GetRelationshipsByStatusesUseCase,
    private val getRelationshipActionUseCase: GetRelationshipActionUseCase,
    private val formatFriendSearchResultsUseCase: FormatFriendSearchResultsUseCase,
    private val acceptFriendRequestUseCase: AcceptFriendRequestUseCase,
    private val removeFriendUseCase: RemoveFriendUseCase,
    private val removeRelationshipUseCase: RemoveRelationshipUseCase,
    private val userRepository: UserRepository,
    private val shareManager: ShareManager,
    private val observeNewPostEvent: ObserveNewPostEventUseCase,
    private val postRepository: PostRepository,
    private val shouldMarkLatestPostAsSeenUseCase: ShouldMarkLatestPostAsSeenUseCase,
    private val mapPostReactionUsersUseCase: MapPostReactionUsersUseCase,
    private val connectivityObserver: ConnectivityObserver,
    private val widgetUpdateManager: WidgetUpdateManager,
    private val mediaRepository: MediaRepository
) : ViewModel() {
    val emojiFloatController: EmojiFloatController by lazy { EmojiFloatController() }

    private companion object {
        private const val DEBOUNCE_MS = 500L
    }

    private var lastFriendSearchQuery: String = ""

    private val _uiState = MutableStateFlow(
        HomeUiState(
            cameraState = CameraState(
                hasCameraPermission = permissionManager.hasPermission(Permission.Camera)
            )
        )
    )

    val uiState: StateFlow<HomeUiState> = combine(
        _uiState,
        userRepository.observeMyUserProfile().distinctUntilChanged(),
    ) { state, profileUi ->
        state.copy(
            userProfile = profileUi,
            isFeedFilterEnabled = (state.friendSheetState.relationshipCounts?.acceptedFriendCount
                ?: 0) > 0 && !state.isLoadingPosts
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = _uiState.value
    )

    private val _imageCapture = mutableStateOf<ImageCapture?>(null)

    fun onSnackbarDismissed() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    fun onPostAudienceChange(audience: PostAudience) {
        _uiState.update { it.copy(postAudience = audience) }
    }

    fun onPermissionRequestHandled() {
        _uiState.update { it.copy(pendingPermission = null) }
    }

    fun onScrollToFirstPostHandled() {
        _uiState.update { it.copy(shouldScrollToFirstPost = false) }
    }

    private var lastMarkedPostCreatedAt: Date? = null
    private var currentVisibleIndex: Int = -1

    /** [Post.id] for the item the user is viewing; set when [currentVisibleIndex] changes. */
    private var viewedPostId: String? = null
    private val currentPostVisible: Post?
        get() = _uiState.value.posts.getOrNull(currentVisibleIndex)

    /** Temp posts for retry only: lookup by id to get imagePath/transform/caption. Not in UI state. */
    private var tempPosts: List<Post> = emptyList()

    /** Create-post audience keyed by temp post id (for retry after failure). */
    private val tempPostCreateAudiences = mutableMapOf<String, PostCreateAudience>()

    private var socketSeqNum: Int = -1

    private var newerJob: Job? = null

    private var friendSearchJob: Job? = null

    init {
        loadNewsfeed()
        loadMyFriendList()
        loadUnreadPostsCount()
        observeUnreadPostsUpdates()
        loadQuickChatEmojiSlots()
        observeNetworkReconnect()
    }

    private fun loadQuickChatEmojiSlots() {
        viewModelScope.launch {
            refreshQuickChatEmojiSlots()
        }
    }

    private suspend fun refreshQuickChatEmojiSlots() {
        val recent = postRepository.getQuickChatRecentEmojis()
        _uiState.update {
            it.copy(quickChatEmojiSlots = QuickChatEmojiSlots.mergeForDisplay(recent))
        }
    }

    private fun loadUnreadPostsCount() {
        viewModelScope.launch {
            postRepository.getUnreadPostsCount().onSuccess { count ->
                _uiState.update { it.copy(unreadPostsCount = count) }
            }
        }
    }

    private fun observeUnreadPostsUpdates() {
        observeNewPostEvent().onEach(::handleNewPostUpdate).launchIn(viewModelScope)
    }

    private fun handleNewPostUpdate(event: NewPostUpdate) {
        val currentSeq = socketSeqNum
        if (event.seq > currentSeq) {
            socketSeqNum = event.seq
            onNewPostEvent(event.count)
        }
    }

    private fun onNewPostEvent(count: Int) {
        _uiState.update { it.copy(unreadPostsCount = count) }

        newerJob?.cancel()
        newerJob = viewModelScope.launch {
            runNewerFetch()
        }
    }

    private suspend fun runNewerFetch() {
        val currentState = _uiState.value
        val unread = currentState.unreadPostsCount
        when (val result = fetchNewerFeedUseCase(
            unreadCount = unread,
            currentPosts = currentState.posts,
        )) {
            is NewerFeedResult.NewPosts -> {
                val merged = result.mergedHead + result.tail
                val showBanner = currentVisibleIndex >= 0 && unread > 0

                val displayUnreadCount = min(unread, 9)
                _uiState.update {
                    it.copy(
                        posts = merged,
                        bannerMessage = if (showBanner) {
                            UiText.PluralResource(
                                R.plurals.new_posts_banner,
                                displayUnreadCount,
                                args = listOf(displayUnreadCount),
                            )
                        } else {
                            null
                        },
                    )
                }
            }

            is NewerFeedResult.Refresh -> loadNewsfeed(isLoadMore = false)

            is NewerFeedResult.Empty -> Unit
        }
    }

    fun onNewPostsBannerTapped() {
        newerJob?.cancel()
        _uiState.update {
            it.copy(
                bannerMessage = null,
                shouldScrollToFirstPost = true,
            )
        }
    }

    private fun tryMarkSeen() {
        if (currentVisibleIndex < 0) return
        if (currentVisibleIndex > FetchNewerFeedUseCase.FEED_MAX_PAGE_INDEX) return

        val state = _uiState.value
        val viewedPost = viewedPostId?.let { id -> state.posts.firstOrNull { it.id == id } }
            ?: state.posts.getOrNull(currentVisibleIndex) ?: return
        val postToMark = shouldMarkLatestPostAsSeenUseCase(
            posts = state.posts,
            lastSeenPostCreatedAt = lastMarkedPostCreatedAt,
            viewedPost = viewedPost,
        ) ?: return
        lastMarkedPostCreatedAt = postToMark.createdAt
        onFeedViewed(postToMark)
    }

    private fun updateFriendSheetState(transform: (FriendBottomSheetState) -> FriendBottomSheetState) {
        _uiState.update { it.copy(friendSheetState = transform(it.friendSheetState)) }
    }

    fun showFriendSheet() {
        _uiState.update { it.copy(showFriendSheet = true) }
    }

    fun onFeedFilterUserSelected(userId: String?) {
        _uiState.update { it.copy(feedUserIdFilter = userId) }
        loadNewsfeed(isLoadMore = false)
    }

    fun onFriendSheetDismissed() {
        friendSearchJob?.cancel()
        friendSearchJob = null
        lastFriendSearchQuery = ""

        _uiState.update { state ->
            state.copy(
                showFriendSheet = false, friendSheetState = state.friendSheetState.copy(
                    isSearchingUsers = false,
                    searchResults = emptyList(),
                )
            )
        }
    }

    fun onViewModeToggle(mode: PostListViewMode) {
        _uiState.update { it.copy(postListViewMode = mode) }
        if (mode == PostListViewMode.GRID) {
            ensureGridFeedCapacity()
        }
    }

    fun resetFeedFilterFromGridCapture() {
        if (_uiState.value.feedUserIdFilter == null) return
        _uiState.update { it.copy(feedUserIdFilter = null) }
        loadNewsfeed(isLoadMore = false)
    }

    fun onGridItemClick(index: Int) {
        _uiState.update {
            it.copy(
                pagerInitialIndex = index,
                postListViewMode = PostListViewMode.PAGER,
            )
        }
    }

    fun onGridNearEndReached() {
        val state = _uiState.value
        if (state.postListViewMode != PostListViewMode.GRID) return
        loadNewsfeed(isLoadMore = true, limit = GRID_LOAD_MORE_STEP)
    }

    private fun ensureGridFeedCapacity() {
        val state = _uiState.value
        if (state.isLoadingPosts || state.isLoadingMore || state.nextCursor == null) return

        val loadLimit = getNewsfeedUseCase.gridInitialTopUpLimit(state.posts.size) ?: return
        loadNewsfeed(isLoadMore = true, limit = loadLimit)
    }

    private fun refreshFriendSearchResults() {
        val query = lastFriendSearchQuery
        if (query.isBlank()) return

        friendSearchJob?.cancel()
        friendSearchJob = null

        viewModelScope.launch {
            val wasLoading = _uiState.value.friendSheetState.isSearchingUsers
            if (!wasLoading) {
                updateFriendSheetState { it.copy(isSearchingUsers = true) }
            }

            val currentUserId = userRepository.getCurrentUserProfile()?.id
            userRepository.searchUsersByUsernamePrefix(query).onSuccess { users ->
                updateFriendSheetState {
                    it.copy(
                        searchResults = formatFriendSearchResultsUseCase(users, currentUserId),
                        isSearchingUsers = false,
                    )
                }
            }.onFailure {
                updateFriendSheetState { it.copy(isSearchingUsers = false) }
            }
        }
    }

    private fun onFeedViewed(post: Post) {
        viewModelScope.launch {
            postRepository.markPostsSeen(post.createdAt).onSuccess {
                widgetUpdateManager.scheduleImmediateUpdate()
            }
        }
    }

    fun loadShareApps() {
        viewModelScope.launch(Dispatchers.Default) {
            val apps = shareManager.getTopShareApps()
            updateFriendSheetState { it.copy(shareApps = apps) }
        }
    }

    fun loadMyFriendList() {
        viewModelScope.launch {
            updateFriendSheetState { it.copy(isLoadingFriendList = true) }
            getRelationshipsByStatusesUseCase(
                listOf(
                    RelationshipStatus.ACCEPTED, RelationshipStatus.PENDING
                )
            ).onSuccess { list ->
                val accepted = list.filter { it.status == RelationshipStatus.ACCEPTED }
                val pending = list.filter { it.status == RelationshipStatus.PENDING }
                val pendingWithActions = coroutineScope {
                    pending.map { item ->
                        async {
                            RelationshipActionItemState(
                                relationship = item,
                                action = getRelationshipActionUseCase(item.userId),
                            )
                        }
                    }.awaitAll()
                }
                updateFriendSheetState {
                    it.copy(
                        friendList = accepted,
                        pendingList = pendingWithActions,
                        relationshipCounts = RelationshipCounts(
                            acceptedFriendCount = accepted.size,
                            pendingRequestCount = incomingPendingCount(pendingWithActions),
                        ),
                        loading = it.loading.copy(initialFriendList = false),
                        isLoadingFriendList = false
                    )
                }
            }.onFailure {
                updateFriendSheetState {
                    it.copy(
                        loading = it.loading.copy(initialFriendList = false),
                        isLoadingFriendList = false
                    )
                }
            }
        }
    }

    fun onFriendSearchQueryChanged(query: String) {
        val trimmed = query.trim()
        lastFriendSearchQuery = trimmed

        friendSearchJob?.cancel()

        if (trimmed.isBlank()) {
            updateFriendSheetState {
                it.copy(
                    isSearchingUsers = false,
                    searchResults = emptyList(),
                )
            }
            return
        }

        // Show loading immediately while waiting for debounce.
        updateFriendSheetState { it.copy(isSearchingUsers = true) }

        friendSearchJob = viewModelScope.launch {
            delay(DEBOUNCE_MS)

            userRepository.searchUsersByUsernamePrefix(trimmed).onSuccess { users ->
                val currentUserId = userRepository.getCurrentUserProfile()?.id

                val formatted = formatFriendSearchResultsUseCase(users, currentUserId)

                updateFriendSheetState {
                    it.copy(
                        searchResults = formatted,
                        isSearchingUsers = false,
                    )
                }
            }.onFailure { _ ->
                updateFriendSheetState {
                    it.copy(
                        isSearchingUsers = false,
                        searchResults = emptyList(),
                    )
                }
            }
        }
    }

    fun acceptFriendRequest(pending: RelationshipWithUser) {
        viewModelScope.launch {
            acceptFriendRequestUseCase(pending.id).onSuccess {
                val acceptedRelationship = pending.copy(status = RelationshipStatus.ACCEPTED)
                updateFriendSheetState { state ->
                    val newPendingList =
                        state.pendingList.filterNot { it.relationship.id == pending.id }
                    val newFriendList = state.friendList + acceptedRelationship
                    state.copy(
                        pendingList = newPendingList,
                        friendList = newFriendList,
                        relationshipCounts = RelationshipCounts(
                            acceptedFriendCount = newFriendList.size,
                            pendingRequestCount = incomingPendingCount(newPendingList),
                        ),
                    )
                }
                loadNewsfeed(isLoadMore = false)
            }.onFailure { error ->
                _uiState.update { it.copy(snackbarMessage = UiText.DynamicString(error.message)) }
            }
        }
    }

    fun sendFriendRequest(targetUserId: String) {
        if (targetUserId.isBlank()) return

        viewModelScope.launch {
            updateFriendSheetState { state ->
                state.copy(loading = state.loading.copy(addingUserIds = state.loading.addingUserIds + targetUserId))
            }
            userRepository.sendFriendRequest(targetUserId).onSuccess {
                loadMyFriendList()
                refreshFriendSearchResults()
            }.onFailure { error ->
                Logger.e("❌ Failed to send friend request: ${error.message}")
            }.also {
                updateFriendSheetState { state ->
                    state.copy(
                        loading = state.loading.copy(
                            addingUserIds = state.loading.addingUserIds - targetUserId
                        )
                    )
                }
            }
        }
    }

    fun requestRemoveFriend(friend: RelationshipWithUser) {
        OverlayEventBus.showConfirmDialog(
            title = UiText.StringResource(
                R.string.remove_friend_confirm_title, listOf(friend.displayName)
            ),
            message = UiText.StringResource(R.string.remove_friend_confirm_message),
            confirmText = UiText.StringResource(R.string.delete),
            cancelText = UiText.StringResource(R.string.cancel),
            confirmDestructive = true,
            onConfirm = { removeFriend(friend) },
        )
    }

    private fun removeFriend(friend: RelationshipWithUser) {
        viewModelScope.launch {
            updateFriendSheetState { state ->
                state.copy(
                    loading = state.loading.copy(
                        removingRelationshipIds = state.loading.removingRelationshipIds + friend.id
                    )
                )
            }
            val state = _uiState.value.friendSheetState
            if (friend.status == RelationshipStatus.PENDING) {
                removeRelationshipUseCase(friend.id).onSuccess {
                    updateFriendSheetState { s ->
                        val newPendingList =
                            s.pendingList.filterNot { it.relationship.id == friend.id }
                        s.copy(
                            pendingList = newPendingList, relationshipCounts = RelationshipCounts(
                                acceptedFriendCount = s.friendList.size,
                                pendingRequestCount = incomingPendingCount(newPendingList),
                            )
                        )
                    }
                    refreshFriendSearchResults()
                }.onFailure { error ->
                    _uiState.update { it.copy(snackbarMessage = UiText.DynamicString(error.message)) }
                }
            } else {
                val currentAccepted = state.relationshipCounts?.acceptedFriendCount
                removeFriendUseCase(friend.id, currentAccepted).onSuccess {
                    _uiState.update { home ->
                        val s = home.friendSheetState
                        val newFriendList = s.friendList.filterNot { it.id == friend.id }
                        val newFilter = if (home.feedUserIdFilter == friend.userId) null
                        else home.feedUserIdFilter
                        home.copy(
                            feedUserIdFilter = newFilter, friendSheetState = s.copy(
                                friendList = newFriendList, relationshipCounts = RelationshipCounts(
                                    acceptedFriendCount = newFriendList.size,
                                    pendingRequestCount = incomingPendingCount(s.pendingList),
                                )
                            )
                        )
                    }
                    refreshFriendSearchResults()
                    loadNewsfeed(isLoadMore = false)
                }.onFailure { error ->
                    _uiState.update { it.copy(snackbarMessage = UiText.DynamicString(error.message)) }
                }
            }
            updateFriendSheetState { current ->
                current.copy(
                    loading = current.loading.copy(
                        removingRelationshipIds = current.loading.removingRelationshipIds - friend.id
                    )
                )
            }
        }
    }

    fun shareToApp(app: ShareApp) {
        viewModelScope.launch {
            val userName =
                userRepository.getCurrentUserProfile()?.userName?.takeIf { it.isNotBlank() }
            val content = shareManager.buildInviteShareContent(userName)
            shareManager.shareToApp(app.packageName, content)
        }
    }

    fun shareOther() {
        viewModelScope.launch {
            val userName =
                userRepository.getCurrentUserProfile()?.userName?.takeIf { it.isNotBlank() }
            val content = shareManager.buildInviteShareContent(userName)
            shareManager.openSystemChooser(content)
        }
    }

    private fun observeNetworkReconnect() {
        connectivityObserver.isInternetAvailable.onEach { isAvailable ->
            if (!isAvailable) return@onEach
            loadMyFriendList()

            val state = _uiState.value
            if (state.isLoadingPosts || state.isLoadingMore || state.posts.isNotEmpty()) return@onEach

            loadNewsfeed(isLoadMore = false)
        }.launchIn(viewModelScope)
    }

    private fun loadNewsfeed(
        isLoadMore: Boolean = false,
        limit: Int = FEED_PAGE_LIMIT,
    ) {
        val state = _uiState.value

        CrashlyticsLogger.action("loadNewsFeed", isLoadMore.toString())

        if (isLoadMore) {
            if (state.isLoadingMore || state.nextCursor == null) return
        } else if (state.isLoadingPosts) return

        viewModelScope.launch {
            _uiState.update {
                if (isLoadMore) it.copy(isLoadingMore = true)
                else it.copy(isLoadingPosts = true)
            }

            val cursor = if (isLoadMore) state.nextCursor else null
            val userId = state.feedUserIdFilter

            getNewsfeedUseCase(
                limit = limit,
                cursor = cursor,
                userId = userId,
            ).fold(onSuccess = { feedData ->
                _uiState.update {
                    it.copy(
                        posts = if (isLoadMore) it.posts + feedData.data else feedData.data,
                        isLoadingPosts = false,
                        isLoadingMore = false,
                        nextCursor = feedData.pagination.nextCursor,
                        error = null
                    )
                }
            }, onFailure = { apiError ->
                _uiState.update {
                    it.copy(
                        isLoadingPosts = false,
                        isLoadingMore = false,
                        error = apiError.message,
                    )
                }
            })
        }
    }

    fun onItemVisible(currentIndex: Int) {
        val previousIndex = currentVisibleIndex
        currentVisibleIndex = currentIndex
        if (currentIndex != previousIndex) {
            emojiFloatController.cancelTrackedAnimation()
        }

        val posts = _uiState.value.posts
        when {
            currentIndex < 0 -> viewedPostId = null
            currentIndex != previousIndex -> viewedPostId = posts.getOrNull(currentIndex)?.id
        }

        tryMarkSeen()

        val state = _uiState.value
        val shouldLoad = shouldTriggerLoadMoreUseCase(
            currentIndex = currentIndex,
            totalItems = state.posts.size,
            canLoadMore = state.canLoadMore
        )
        if (shouldLoad) {
            loadNewsfeed(
                isLoadMore = true,
                limit = FEED_PAGE_LIMIT,
            )
        }

        val visiblePost = state.posts.getOrNull(currentIndex)
        if (visiblePost != null && visiblePost.isOwnPost) {
            loadPostReactions(visiblePost.id, visiblePost.isOwnerViewedPost)
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

    private var reactToPostJob: Job? = null

    fun onEmojiReaction(emoji: String) {
        emojiFloatController.emit(emoji)

        val postIdToReact = viewedPostId ?: return

        reactToPostJob?.cancel()
        reactToPostJob = viewModelScope.launch {
            delay(DEBOUNCE_MS)

            runCatching { postRepository.recordQuickChatEmojiUsage(emoji) }.onFailure { error ->
                Logger.e("Failed to persist quick chat emoji usage: ${error.message}")
            }
            postRepository.reactToPost(postId = postIdToReact, reactionIcon = emoji)
                .onFailure { error ->
                    Logger.e("Failed to react to post: ${error.message}")
                }
        }
    }

    private var postReactionsJob: Job? = null

    private fun loadPostReactions(postId: String, isOwnerViewed: Boolean = false) {
        postReactionsJob?.cancel()
        _uiState.update { it.copy(postReactionsState = PostReactionsUiState.Loading) }
        postReactionsJob = viewModelScope.launch {
            postRepository.getPostReactions(postId).fold(onSuccess = { reactions ->
                val mapped =
                    runCatching { mapPostReactionUsersUseCase(reactions) }.onFailure { error ->
                        Logger.e("Failed to map post reactions for postId=$postId: ${error.message}")
                    }.getOrDefault(emptyList())
                _uiState.update { it.copy(postReactionsState = PostReactionsUiState.Result(mapped)) }

                if (!isOwnerViewed) {
                    val emoji = mapped.firstOrNull()?.reactionIcons?.firstOrNull()
                    if (emoji != null) {
                        emojiFloatController.emit(
                            emoji = emoji,
                            direction = FloatDirection.DOWN,
                            onEnd = {
                                markPostOwnerViewed(postId)
                            },
                        )
                    }
                }
            }, onFailure = {
                _uiState.update {
                    it.copy(
                        postReactionsState = PostReactionsUiState.Result(emptyList())
                    )
                }
            })
        }
    }

    private fun markPostOwnerViewed(postId: String) {
        _uiState.update { state ->
            state.copy(
                posts = state.posts.map { post ->
                    if (post.id == postId) post.copy(isOwnerViewedPost = true) else post
                })
        }
        viewModelScope.launch {
            postRepository.markPostOwnerViewed(postId)
        }
    }

    fun onSwitchCamera() {
        updateCameraState { state ->
            val newLens = if (state.lensFacing == CameraSelector.LENS_FACING_BACK) {
                CameraSelector.LENS_FACING_FRONT
            } else {
                CameraSelector.LENS_FACING_BACK
            }
            state.copy(lensFacing = newLens, lastPreviewSnapshot = null)
        }
    }

    private fun updateCameraState(transform: (CameraState) -> CameraState) {
        _uiState.update { state ->
            state.copy(cameraState = transform(state.cameraState))
        }
    }

    fun updateCurrentCaption(caption: String) {
        _uiState.update { it.copy(currentCaption = caption) }
    }

    fun onRequestCameraPermission() {
        val hasPermission = permissionManager.hasPermission(Permission.Camera)
        updateCameraState { it.copy(hasCameraPermission = hasPermission) }

        if (!hasPermission) {
            _uiState.update { it.copy(pendingPermission = Permission.Camera) }
        }
    }

    fun onPermissionResult(granted: Boolean) {
        updateCameraState { it.copy(hasCameraPermission = granted) }
        if (!granted) {
            _uiState.update { it.copy(snackbarMessage = UiText.DynamicString("Camera permission is required")) }
        }
    }

    fun setImageCapture(capture: ImageCapture) {
        _imageCapture.value = capture
        updateCameraState { it.copy(isCameraActive = true) }
    }

    fun setPreviewSnapshot(bitmap: Bitmap) {
        updateCameraState { it.copy(lastPreviewSnapshot = bitmap) }
    }

    fun onCameraPageVisible() {
        updateCameraState { it.copy(shouldBindCamera = true) }
    }

    fun onCameraPageHidden() {
        updateCameraState { it.copy(shouldBindCamera = false) }
    }

    fun onCapturePhoto(context: Context) {
        val cameraState = _uiState.value.cameraState
        when (validateCaptureReadinessUseCase(
            cameraState.hasCameraPermission, cameraState.isCameraActive
        )) {
            CaptureReadiness.NeedPermission -> _uiState.update { it.copy(pendingPermission = Permission.Camera) }
            CaptureReadiness.CameraNotReady -> _uiState.update {
                it.copy(snackbarMessage = UiText.DynamicString("Camera is not ready"))
            }

            CaptureReadiness.Ready -> {
                val counts = _uiState.value.friendSheetState.relationshipCounts
                if (counts != null && counts.acceptedFriendCount > 0) {
                    takePhoto(context)
                } else {
                    OverlayEventBus.showConfirmDialog(
                        title = UiText.StringResource(R.string.capture_no_friends_dialog_title),
                        message = UiText.StringResource(R.string.capture_no_friends_dialog_message),
                        confirmText = UiText.StringResource(R.string.capture_no_friends_dialog_add_friend),
                        cancelText = UiText.StringResource(R.string.ok),
                        onConfirm = { showFriendSheet() },
                    )
                }
            }
        }
    }

    private fun takePhoto(context: Context) {
        val capture = _imageCapture.value ?: run {
            _uiState.update { it.copy(snackbarMessage = UiText.DynamicString("Camera is not ready")) }
            return
        }

        val executor = ContextCompat.getMainExecutor(context)
        updateCameraState { it.copy(isCapturing = true) }

        val photoFile = File(
            context.cacheDir, SimpleDateFormat(
                "yyyy-MM-dd-HH-mm-ss-SSS", Locale.US
            ).format(System.currentTimeMillis()) + ".jpg"
        )
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        capture.takePicture(
            outputOptions, executor, object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    viewModelScope.launch {
                        withContext(Dispatchers.Main.immediate) {
                            _uiState.update { state ->
                                state.copy(
                                    cameraState = state.cameraState.copy(
                                        isCapturing = false,
                                        capturedImagePath = photoFile.absolutePath,
                                    ),
                                    postAudience = PostAudience.FriendOnly,
                                )
                            }
                        }
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    updateCameraState { it.copy(isCapturing = false) }
                    _uiState.update { it.copy(snackbarMessage = UiText.DynamicString("Failed to capture photo")) }
                }
            })
    }

    fun onCancelCapture() {
        val state = _uiState.value.cameraState
        val imagePath = state.capturedImagePath
        FileUtils.deleteFileFromPath(imagePath)
        updateCameraState { it.copy(capturedImagePath = null, pickedImageUri = null) }
        _uiState.update { it.copy(currentCaption = null) }
    }

    fun onGalleryImagePicked(uri: Uri) {
        updateCameraState { it.copy(capturedImagePath = null, pickedImageUri = uri.toString()) }
        viewModelScope.launch {
            val oldPath = _uiState.value.cameraState.capturedImagePath
            FileUtils.deleteFileFromPath(oldPath)
        }
    }

    fun onUploadPost() {
        viewModelScope.launch {
            val state = _uiState.value
            val isPickedFromGallery =
                state.cameraState.capturedImagePath == null && state.cameraState.pickedImageUri != null
            if (isPickedFromGallery) {
                val importedPath = mediaRepository.importPickedImageToCache(
                    uri = state.cameraState.pickedImageUri.toUri()
                ).getOrElse { e ->
                    Logger.e(e, "Import picked image failed")
                    _uiState.update { it.copy(snackbarMessage = UiText.DynamicString("Failed to import image")) }
                    return@launch
                }

                updateCameraState {
                    it.copy(
                        capturedImagePath = importedPath, pickedImageUri = null
                    )
                }
            }

            val latestState = _uiState.value
            val isUploading = latestState.uploadStatuses.values.any { it is UploadStatus.Uploading }
            when (val result = validateUploadPostUseCase(
                capturedImagePath = latestState.cameraState.capturedImagePath,
                caption = latestState.currentCaption,
                isUploading = isUploading
            )) {
                is ValidateUploadPostUseCase.ValidateUploadResult.Success -> {
                    val input = result.input
                    val tempPostId = "temp_${System.currentTimeMillis()}"
                    val createAudience = mapPostAudience(latestState.postAudience)

                    _uiState.update { s ->
                        s.copy(
                            uploadStatuses = s.uploadStatuses + (tempPostId to UploadStatus.Uploading),
                        )
                    }

                    val isFrontCamera =
                        state.cameraState.lensFacing == CameraSelector.LENS_FACING_FRONT
                    val shouldFlipHorizontal = isFrontCamera && !isPickedFromGallery

                    val processedPath = withContext(Dispatchers.IO) {
                        FileUtils.flipAndCompressImage(
                            File(input.imagePath), flipHorizontal = shouldFlipHorizontal
                        ) ?: input.imagePath
                    }
                    val transform = ImageTransform(rotation = 0, scaleX = 1f, scaleY = 1f)
                    val tempPost = createTempPostUseCase(
                        id = tempPostId,
                        imagePath = processedPath,
                        userProfile = input.userProfile,
                        transform = transform,
                        caption = input.caption,
                        createAudience = createAudience,
                    )

                    tempPostCreateAudiences[tempPostId] = createAudience
                    tempPosts = tempPosts + tempPost
                    _uiState.update { s ->
                        s.copy(
                            posts = listOf(tempPost) + s.posts,
                            cameraState = s.cameraState.copy(capturedImagePath = null),
                            currentCaption = null
                        )
                    }

                    _uiState.update { it.copy(shouldScrollToFirstPost = true) }
                    runUploadAndUpdateStatus(
                        tempPostId,
                        processedPath,
                        transform,
                        input.caption,
                        createAudience,
                    )
                }

                is ValidateUploadPostUseCase.ValidateUploadResult.AlreadyUploading -> { /* no-op, already uploading */
                }

                is ValidateUploadPostUseCase.ValidateUploadResult.NoImage -> {
                    _uiState.update { it.copy(snackbarMessage = UiText.DynamicString("No image to upload")) }
                }

                is ValidateUploadPostUseCase.ValidateUploadResult.UserProfileNotFound -> {
                    _uiState.update { it.copy(snackbarMessage = UiText.DynamicString("User profile not found")) }
                }
            }
        }
    }

    private fun runUploadAndUpdateStatus(
        tempPostId: String,
        imagePath: String,
        transform: ImageTransform,
        caption: String?,
        createAudience: PostCreateAudience,
    ) {
        viewModelScope.launch {
            when (
                val result = uploadPostUseCase(
                    imagePath,
                    transform,
                    caption,
                    createAudience,
                )
            ) {
                is UploadPostResult.Success -> {
                    val realPostId = result.post.id
                    tempPosts = tempPosts.filterNot { it.id == tempPostId }
                    tempPostCreateAudiences.remove(tempPostId)
                    _uiState.update { state ->
                        state.copy(
                            posts = state.posts.map { if (it.id == tempPostId) it.copy(id = realPostId) else it },
                            uploadStatuses = state.uploadStatuses - tempPostId,
                            // snackbarMessage = UiText.DynamicString("Post uploaded successfully")
                        )
                    }
                }

                is UploadPostResult.Failed -> {
                    showUploadLimitReachedDialogIfNeeded(result)
                    setUploadStatus(tempPostId, UploadStatus.Failed(result.message))
                }
            }
        }
    }

    private fun showUploadLimitReachedDialogIfNeeded(result: UploadPostResult.Failed): Boolean {
        val apiError = result.apiError ?: return false
        if (apiError.errorCode != ApiErrorCode.POST_CREATE_LIMIT_EXCEEDED) return false

        val hoursRemaining = (apiError.hoursRemaining ?: 24).coerceAtLeast(1)
        OverlayEventBus.showConfirmDialog(
            title = UiText.StringResource(R.string.upload_post_limit_title),
            message = UiText.StringResource(
                R.string.upload_post_limit_message,
                listOf(hoursRemaining),
            ),
            confirmText = UiText.StringResource(R.string.ok),
            onConfirm = {},
        )
        return true
    }

    fun onShowMoreOptions() {
        currentPostVisible?.let { onShowMoreOptions(it) }
    }

    fun onShowMoreOptions(post: Post) {
        val isUploading = _uiState.value.uploadStatuses[post.id] == UploadStatus.Uploading
        val actions = getAvailablePostActionsUseCase(post, isUploading)

        val options = actions.map { action ->
            when (action) {
                is PostAction.Share -> SheetOption(
                    id = "share",
                    label = UiText.StringResource(R.string.share),
                    onClick = { sharePost(post) })

                is PostAction.Download -> SheetOption(
                    id = "download",
                    label = UiText.StringResource(R.string.download),
                    onClick = { downloadPostImage(post) })

                is PostAction.Delete -> SheetOption(
                    id = "delete",
                    label = UiText.StringResource(R.string.delete),
                    color = Error50,
                    onClick = {
                        OverlayEventBus.showConfirmDialog(
                            title = UiText.StringResource(R.string.delete_photo_title),
                            message = UiText.StringResource(R.string.delete_photo_message),
                            confirmText = UiText.StringResource(R.string.delete),
                            onConfirm = { deletePost(post.id) },
                            confirmDestructive = true
                        )
                    })

                is PostAction.Report -> SheetOption(
                    id = "report",
                    label = UiText.StringResource(R.string.report),
                    color = Error50,
                    onClick = {})

                is PostAction.Cancel -> SheetOption(
                    id = "cancel",
                    label = UiText.StringResource(R.string.cancel),
                    onClick = { /* dismiss only */ })
            }
        }
        OverlayEventBus.showOptionsSheet(options = options)
    }

    private fun sharePost(post: Post) {
        val media = post.media.firstOrNull() ?: run {
            _uiState.update { it.copy(snackbarMessage = UiText.DynamicString("Không tìm thấy ảnh để share")) }
            return
        }
        val imageSource = media.images.original
        if (imageSource.isBlank()) {
            _uiState.update { it.copy(snackbarMessage = UiText.DynamicString("Không tìm thấy ảnh để share")) }
            return
        }

        if (_uiState.value.isDownloading) return

        _uiState.update { it.copy(isDownloading = true) }

        viewModelScope.launch {
            val uriResult = mediaRepository.prepareShareImageUri(imageSource)
            uriResult.onSuccess { uri ->
                val content = buildPostShareContentUseCase(post.firstName, post.id)
                shareManager.openSystemChooser(content = content, imageUri = uri)
                _uiState.update { it.copy(isDownloading = false) }
            }.onFailure { e ->
                _uiState.update {
                    it.copy(
                        isDownloading = false,
                        snackbarMessage = UiText.DynamicString(e.message ?: "Không share được ảnh")
                    )
                }
            }
        }
    }

    fun retryUpload(tempPostId: String) {
        val tempPost = tempPosts.find { it.id == tempPostId }
        when (val result = validateRetryUploadUseCase(tempPost)) {
            is ValidateRetryUploadUseCase.ValidateRetryResult.Success -> {
                val input = result.input
                setUploadStatus(input.tempPostId, UploadStatus.Uploading)
                val createAudience = tempPostCreateAudiences[input.tempPostId]
                    ?: PostCreateAudience.FriendOnly
                runUploadAndUpdateStatus(
                    input.tempPostId,
                    input.imagePath,
                    input.transform,
                    input.caption,
                    createAudience,
                )
            }

            is ValidateRetryUploadUseCase.ValidateRetryResult.PostNotFound -> {
                _uiState.update { it.copy(snackbarMessage = UiText.DynamicString("Cannot retry upload: Post data not found")) }
            }

            is ValidateRetryUploadUseCase.ValidateRetryResult.MediaNotFound -> {
                _uiState.update { it.copy(snackbarMessage = UiText.DynamicString("Cannot retry upload: Media not found")) }
            }

            is ValidateRetryUploadUseCase.ValidateRetryResult.ImagePathNotFound -> {
                _uiState.update { it.copy(snackbarMessage = UiText.DynamicString("Cannot retry upload: Image path not found")) }
            }
        }
    }

    private fun setUploadStatus(tempPostId: String, status: UploadStatus) {
        _uiState.update { state ->
            state.copy(uploadStatuses = state.uploadStatuses + (tempPostId to status))
        }
    }

    fun deleteFailedPost(tempPostId: String) {
        tempPosts = tempPosts.filterNot { it.id == tempPostId }
        removeTempPost(tempPostId)
    }

    private fun removeTempPost(tempPostId: String) {
        tempPostCreateAudiences.remove(tempPostId)
        _uiState.update { state ->
            state.copy(
                posts = state.posts.filterNot { it.id == tempPostId },
                uploadStatuses = state.uploadStatuses - tempPostId
            )
        }
    }

    private fun deletePost(postId: String) {
        viewModelScope.launch {
            deletePostUseCase(postId).onSuccess {
                tempPosts = tempPosts.filterNot { it.id == postId }
                _uiState.update { state ->
                    state.copy(
                        posts = state.posts.filterNot { it.id == postId },
                        uploadStatuses = state.uploadStatuses - postId
                    )
                }
            }.onFailure { error ->
                _uiState.update { it.copy(snackbarMessage = UiText.DynamicString(error.message)) }
            }
        }
    }

    fun downloadPostImage(post: Post) {
        if (_uiState.value.isDownloading) return
        val media = post.media.firstOrNull() ?: run {
            _uiState.update { it.copy(snackbarMessage = UiText.StringResource(R.string.download_failed)) }
            return
        }
        val imageSource = media.images.original

        _uiState.update { it.copy(isDownloading = true) }
        viewModelScope.launch {
            mediaRepository.downloadImage(imageSource).onSuccess {
                _uiState.update {
                    it.copy(
                        isDownloading = false
                    )
                }
            }.onFailure { e ->
                _uiState.update {
                    it.copy(
                        isDownloading = false,
                        snackbarMessage = UiText.DynamicString(e.message ?: "Download failed")
                    )
                }
            }
        }
    }

    fun downloadCaptureImage() {
        val state = _uiState.value
        val imageSource = state.cameraState.capturedImagePath
        if (state.isDownloading || imageSource == null) return

        _uiState.update { it.copy(isDownloading = true) }

        viewModelScope.launch {
            val isFrontCamera = state.cameraState.lensFacing == CameraSelector.LENS_FACING_FRONT

            val processedPath = withContext(Dispatchers.IO) {
                FileUtils.flipAndCompressImage(
                    File(imageSource), flipHorizontal = isFrontCamera
                ) ?: imageSource
            }

            mediaRepository.downloadImage(processedPath).onSuccess {
                _uiState.update {
                    it.copy(
                        isDownloading = false
                    )
                }
            }.onFailure { e ->
                _uiState.update {
                    it.copy(
                        isDownloading = false,
                        snackbarMessage = UiText.DynamicString(e.message ?: "Download failed")
                    )
                }
            }
        }
    }

    /**
     * Count for [RelationshipCounts.pendingRequestCount] when built here: incoming requests only
     * ([RelationshipAction.PendingByOther]), not outgoing [RelationshipAction.PendingByMe].
     */
    private fun incomingPendingCount(pending: List<RelationshipActionItemState>): Int =
        pending.count { it.action is RelationshipAction.PendingByOther }

    private fun mapPostAudience(audience: PostAudience): PostCreateAudience = when (audience) {
        PostAudience.FriendOnly -> PostCreateAudience.FriendOnly
        is PostAudience.SelectedFriends ->
            PostCreateAudience.SelectedUsers(audience.friends.map { it.userId })
    }

}