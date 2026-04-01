package com.thinh.snaplet.ui.screens.home.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.thinh.snaplet.R
import com.thinh.snaplet.data.model.RelationshipStatus
import com.thinh.snaplet.data.model.RelationshipWithUser
import com.thinh.snaplet.data.model.user.AvatarUrls
import com.thinh.snaplet.data.model.user.UserSearchResult
import com.thinh.snaplet.domain.model.RelationshipAction
import com.thinh.snaplet.platform.share.ShareApp
import com.thinh.snaplet.ui.components.AppIconButton
import com.thinh.snaplet.ui.components.Avatar
import com.thinh.snaplet.ui.components.BaseText
import com.thinh.snaplet.ui.components.BaseTextField
import com.thinh.snaplet.ui.components.IconSpec
import com.thinh.snaplet.ui.components.PrimaryButton
import com.thinh.snaplet.ui.screens.home.FriendBottomSheetState
import com.thinh.snaplet.ui.theme.MotionTokens
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val MAX_FRIENDS_DISPLAY = 30
private val ICON_BUTTON_SIZE = 52.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendBottomSheet(
    onDismiss: () -> Unit,
    friendSheetState: FriendBottomSheetState,
    onShareToApp: (ShareApp) -> Unit,
    onShareOther: () -> Unit,
    onSheetVisible: () -> Unit = {},
    searchQuery: String = "",
    onSearchQueryChange: (String) -> Unit = {},
    onFriendRemove: (RelationshipWithUser) -> Unit,
    onPendingAccept: (RelationshipWithUser) -> Unit,
    onAddFriend: (String) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isReady by remember(friendSheetState.shareApps, friendSheetState.loading.initialFriendList) {
        derivedStateOf {
            friendSheetState.shareApps.isNotEmpty() && !friendSheetState.loading.initialFriendList
        }
    }

    LaunchedEffect(Unit) {
        onSheetVisible()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.statusBarsPadding(),
    ) {
        if (!isReady) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(modifier = Modifier.size(52.dp))
            }
            return@ModalBottomSheet
        }

        val focusManager = LocalFocusManager.current
        val current = friendSheetState.relationshipCounts?.acceptedFriendCount ?: 0

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { focusManager.clearFocus() })
                }
                .padding(horizontal = 20.dp)
                .navigationBarsPadding(),
        ) {
            item(key = "title") {
                Column(modifier = Modifier.fillMaxWidth()) {
                    BaseText(
                        text = stringResource(R.string.friend_sheet_title),
                        color = MaterialTheme.colorScheme.onBackground,
                        typography = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                    BaseText(
                        text = stringResource(
                            R.string.friend_sheet_count, current, MAX_FRIENDS_DISPLAY
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        typography = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 16.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
            item(key = "search") {
                BaseTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    placeholder = {
                        BaseText(
                            text = stringResource(R.string.friend_sheet_add_placeholder),
                            color = MaterialTheme.colorScheme.onSurface,
                            typography = MaterialTheme.typography.bodyMedium
                        )
                    },
                    leadingIcon = {
                        Crossfade(
                            targetState = friendSheetState.isSearchingUsers,
                            animationSpec = tween(durationMillis = MotionTokens.Emphasized)
                        ) { isSearching ->
                            if (isSearching) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = MaterialTheme.colorScheme.onBackground,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Filled.Search,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    })
            }

            if (friendSheetState.searchResults.isNotEmpty()) {
                item { Spacer(Modifier.size(24.dp)) }

                items(
                    items = friendSheetState.searchResults, key = { it.user.userId }) { item ->
                    FriendSearchListItem(
                        user = item.user,
                        relationshipAction = item.action,
                        onAcceptRequest = {
                            val relationshipId = item.user.relationshipId
                            if (relationshipId.isNullOrBlank()) return@FriendSearchListItem

                            val relationshipStatus =
                                item.user.relationshipStatus?.let { RelationshipStatus.from(it) }
                                    ?: RelationshipStatus.PENDING

                            onPendingAccept(
                                RelationshipWithUser(
                                    id = relationshipId,
                                    userId = item.user.userId,
                                    username = item.user.username,
                                    firstName = item.user.firstName,
                                    lastName = item.user.lastName,
                                    avatarUrls = item.user.avatarUrls,
                                    status = relationshipStatus,
                                    createdAt = item.user.relationshipCreatedAt.orEmpty(),
                                )
                            )
                        },
                        onRemove = {
                            val relationshipId = item.user.relationshipId
                            if (relationshipId.isNullOrBlank()) return@FriendSearchListItem

                            val relationshipStatus =
                                item.user.relationshipStatus?.let { RelationshipStatus.from(it) }
                                    ?: RelationshipStatus.PENDING

                            onFriendRemove(
                                RelationshipWithUser(
                                    id = relationshipId,
                                    userId = item.user.userId,
                                    username = item.user.username,
                                    firstName = item.user.firstName,
                                    lastName = item.user.lastName,
                                    avatarUrls = item.user.avatarUrls,
                                    status = relationshipStatus,
                                    createdAt = item.user.relationshipCreatedAt.orEmpty(),
                                )
                            )
                        },
                        onAddFriend = { onAddFriend(item.user.userId) },
                        isAddingFriend = friendSheetState.loading.addingUserIds.contains(item.user.userId),
                        isRemovingFriend = item.user.relationshipId?.let {
                            friendSheetState.loading.removingRelationshipIds.contains(it)
                        } ?: false,
                    )
                }
            } else {
                item(key = "share_section") {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        BaseText(
                            text = stringResource(R.string.friend_sheet_find_from_apps),
                            color = MaterialTheme.colorScheme.onBackground,
                            typography = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(vertical = 24.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            friendSheetState.shareApps.forEach { app ->
                                ShareAppIconItem(
                                    shareApp = app, onClick = { onShareToApp(app) })
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                AppIconButton(
                                    modifier = Modifier.size(ICON_BUTTON_SIZE),
                                    icon = IconSpec.Vector(
                                        Icons.Outlined.Share,
                                        tint = MaterialTheme.colorScheme.onBackground
                                    ),
                                    onClick = onShareOther,
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    iconSize = ICON_BUTTON_SIZE / 2
                                )
                                BaseText(
                                    text = stringResource(R.string.friend_sheet_other),
                                    color = MaterialTheme.colorScheme.onBackground,
                                    typography = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }
                if (friendSheetState.pendingList.isNotEmpty()) {
                    item(key = "pending_section_title") {
                        BaseText(
                            text = stringResource(R.string.friend_request_section_title),
                            color = MaterialTheme.colorScheme.onBackground,
                            typography = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(vertical = 24.dp)
                        )
                    }
                    items(
                        items = friendSheetState.pendingList,
                        key = { it.relationship.id }) { item ->
                        val friend = item.relationship
                        FriendListItem(
                            relationshipAction = item.action,
                            onAcceptRequest = { onPendingAccept(friend) },
                            onRemove = { onFriendRemove(friend) },
                            displayName = friend.displayName,
                            firstName = friend.firstName,
                            relationshipStatus = friend.status,
                            avatarUrls = friend.avatarUrls,
                            isRemovingFriend = friendSheetState.loading.removingRelationshipIds.contains(friend.id),
                        )
                    }
                }
                if (friendSheetState.friendList.isNotEmpty()) {
                    item(key = "friends_section_title") {
                        BaseText(
                            text = stringResource(R.string.friend_sheet_title),
                            color = MaterialTheme.colorScheme.onBackground,
                            typography = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(vertical = 24.dp)
                        )
                    }
                    items(
                        items = friendSheetState.friendList, key = { it.id }) { friend ->
                        FriendListItem(
                            relationshipAction = RelationshipAction.Accepted,
                            onAcceptRequest = {},
                            onRemove = { onFriendRemove(friend) },
                            displayName = friend.displayName,
                            firstName = friend.firstName,
                            relationshipStatus = friend.status,
                            avatarUrls = friend.avatarUrls,
                            isRemovingFriend = friendSheetState.loading.removingRelationshipIds.contains(friend.id),
                        )
                    }
                }
            }

            item(key = "bottom_padding") {
                Box(modifier = Modifier.padding(bottom = 24.dp))
            }
        }
    }
}

@Composable
private fun FriendSearchListItem(
    user: UserSearchResult,
    relationshipAction: RelationshipAction,
    onAcceptRequest: () -> Unit,
    onRemove: () -> Unit,
    onAddFriend: () -> Unit,
    isAddingFriend: Boolean = false,
    isRemovingFriend: Boolean = false,
) {
    val relationshipStatus =
        user.relationshipStatus?.let { RelationshipStatus.from(it) } ?: RelationshipStatus.PENDING

    val displayName =
        listOf(user.firstName, user.lastName).filter { it.isNotBlank() }.joinToString(" ")
            .ifBlank { user.username }

    val canRemove =
        relationshipAction !is RelationshipAction.AddFriend && relationshipAction !is RelationshipAction.CurrentUser

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Avatar(
            avatarUrl = user.avatarUrls.forThumbnail().ifBlank { null },
            firstName = user.firstName,
            isConnectedUser = relationshipStatus == RelationshipStatus.ACCEPTED,
            borderWidth = 2.dp,
            size = 40.dp
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.Center
        ) {
            BaseText(
                text = displayName,
                color = MaterialTheme.colorScheme.onBackground,
                typography = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            BaseText(
                text = user.username,
                color = MaterialTheme.colorScheme.onSurface,
                typography = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        FriendListItemActionSlot(
            relationshipAction = relationshipAction,
            onAcceptRequest = onAcceptRequest,
            onAddFriend = onAddFriend,
            isAddingFriend = isAddingFriend,
        )
        if (canRemove) {
            Spacer(Modifier.size(8.dp))
            AppIconButton(
                modifier = Modifier.size(ICON_BUTTON_SIZE / 1.3f),
                icon = IconSpec.Vector(
                    Icons.Outlined.Close, tint = MaterialTheme.colorScheme.onBackground
                ),
                onClick = onRemove,
                loading = isRemovingFriend,
                containerColor = MaterialTheme.colorScheme.surface,
                iconSize = ICON_BUTTON_SIZE / 2
            )
        }
    }
}

@Composable
private fun FriendListItem(
    displayName: String,
    firstName: String,
    relationshipStatus: RelationshipStatus,
    avatarUrls: AvatarUrls,
    relationshipAction: RelationshipAction,
    onAcceptRequest: () -> Unit,
    onRemove: () -> Unit,
    isRemovingFriend: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Avatar(
            avatarUrl = avatarUrls.forThumbnail().ifBlank { null },
            firstName = firstName,
            isConnectedUser = relationshipStatus == RelationshipStatus.ACCEPTED,
            borderWidth = 2.dp,
            size = 40.dp
        )
        BaseText(
            text = displayName,
            color = MaterialTheme.colorScheme.onBackground,
            typography = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        FriendListItemActionSlot(
            relationshipAction = relationshipAction,
            onAcceptRequest = onAcceptRequest,
        )
        Spacer(Modifier.size(8.dp))
        AppIconButton(
            modifier = Modifier.size(ICON_BUTTON_SIZE / 1.3f),
            icon = IconSpec.Vector(
                Icons.Outlined.Close, tint = MaterialTheme.colorScheme.onBackground
            ),
            onClick = onRemove,
            loading = isRemovingFriend,
            containerColor = MaterialTheme.colorScheme.surface,
            iconSize = ICON_BUTTON_SIZE / 2
        )
    }
}

@Composable
private fun FriendListItemActionSlot(
    relationshipAction: RelationshipAction,
    onAcceptRequest: () -> Unit,
    onAddFriend: () -> Unit = {},
    isAddingFriend: Boolean = false,
) {
    var isResending by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun fakeResendRequest() {
        scope.launch {
            isResending = true
            delay(1500L)
            isResending = false
        }
    }

    when (relationshipAction) {
        is RelationshipAction.PendingByOther -> {
            PrimaryButton(
                onClick = onAcceptRequest,
                title = stringResource(R.string.accept),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                typography = MaterialTheme.typography.titleSmall,
                titleColor = MaterialTheme.colorScheme.onPrimary,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }

        is RelationshipAction.PendingByMe -> {
            AppIconButton(
                modifier = Modifier.size(ICON_BUTTON_SIZE / 1.3f),
                icon = IconSpec.Vector(
                    Icons.Outlined.Refresh, tint = MaterialTheme.colorScheme.onBackground
                ),
                loading = isResending,
                onClick = ::fakeResendRequest,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onBackground,
                iconSize = ICON_BUTTON_SIZE / 2
            )
        }

        is RelationshipAction.AddFriend -> {
            PrimaryButton(
                onClick = onAddFriend,
                isLoading = isAddingFriend,
                title = stringResource(R.string.add_friend),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                typography = MaterialTheme.typography.titleSmall,
                titleColor = MaterialTheme.colorScheme.onPrimary,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }

        RelationshipAction.Accepted, RelationshipAction.Blocked, RelationshipAction.CurrentUser -> Unit
    }
}

@Composable
private fun ShareAppIconItem(
    shareApp: ShareApp, onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        AppIconButton(
            modifier = Modifier.size(ICON_BUTTON_SIZE),
            icon = IconSpec.AndroidDrawable(shareApp.appIcon),
            onClick = onClick,
            containerColor = if (shareApp.iconBackgroundColor != null) Color(shareApp.iconBackgroundColor) else MaterialTheme.colorScheme.onBackground,
            iconSize = ICON_BUTTON_SIZE
        )
        BaseText(
            text = shareApp.displayName,
            color = MaterialTheme.colorScheme.onBackground,
            typography = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(top = 8.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}