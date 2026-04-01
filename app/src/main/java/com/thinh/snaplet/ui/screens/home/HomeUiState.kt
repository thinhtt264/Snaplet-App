package com.thinh.snaplet.ui.screens.home

import android.graphics.Bitmap
import androidx.camera.core.CameraSelector
import com.thinh.snaplet.data.model.RelationshipCounts
import com.thinh.snaplet.data.model.RelationshipWithUser
import com.thinh.snaplet.data.model.post.Post
import com.thinh.snaplet.domain.model.FriendSearchActionItem
import com.thinh.snaplet.domain.model.ReactionUserUi
import com.thinh.snaplet.domain.model.RelationshipAction
import com.thinh.snaplet.platform.permission.Permission
import com.thinh.snaplet.platform.share.ShareApp
import com.thinh.snaplet.ui.common.UiText

data class RelationshipActionItemState(
    val relationship: RelationshipWithUser,
    val action: RelationshipAction,
)

data class HomeUiState(
    val cameraState: CameraState,
    val currentCaption: String? = null,

    val posts: List<Post> = emptyList(),
    val isLoadingPosts: Boolean = false,
    val isLoadingMore: Boolean = false,

    val nextCursor: String? = null, // Cursor for pagination, null means no more data

    val error: String? = null,

    val friendSheetState: FriendBottomSheetState = FriendBottomSheetState(),

    val uploadStatuses: Map<String, UploadStatus> = emptyMap(),
    val isDownloading: Boolean = false,

    val unreadPostsCount: Int = 0,

    val bannerMessage: UiText? = null,

    val snackbarMessage: UiText? = null,

    val pendingPermission: Permission? = null,

    val shouldScrollToFirstPost: Boolean = false,

    val profileAvatarUrl: String? = null,

    val postReactionsState: PostReactionsUiState = PostReactionsUiState.Loading,
    val showReactionsSheet: Boolean = false,

    val quickChatEmojiSlots: List<String> = QuickChatEmojiSlots.mergeForDisplay(emptyList()),
) {
    /** Returns true if more data can be loaded (nextCursor is not null and not currently loading) */
    val canLoadMore: Boolean get() = nextCursor != null && !isLoadingMore && !isLoadingPosts
}

sealed class PostReactionsUiState {
    object Loading : PostReactionsUiState()
    data class Result(val reactions: List<ReactionUserUi> = emptyList()) : PostReactionsUiState()
}

sealed class UploadStatus {
    object Uploading : UploadStatus()
    data class Failed(val errorMessage: String) : UploadStatus()
    object Success : UploadStatus()
}

data class CameraState(
    val isCameraActive: Boolean = false,
    val isCapturing: Boolean = false,
    val shouldBindCamera: Boolean = true,
    val lastPreviewSnapshot: Bitmap? = null,
    val hasCameraPermission: Boolean = false,
    val lensFacing: Int = CameraSelector.LENS_FACING_FRONT,
    val capturedImagePath: String? = null
) {
    val isEditMode: Boolean get() = capturedImagePath != null
}

data class FriendBottomSheetState(
    val relationshipCounts: RelationshipCounts? = null,
    val friendList: List<RelationshipWithUser> = emptyList(),
    val pendingList: List<RelationshipActionItemState> = emptyList(),
    val isLoadingFriendList: Boolean = false,
    val searchResults: List<FriendSearchActionItem> = emptyList(),
    val isSearchingUsers: Boolean = false,
    val shareApps: List<ShareApp> = emptyList(),
)