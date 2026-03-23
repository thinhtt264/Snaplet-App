package com.thinh.snaplet.ui.screens.friend_request.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.thinh.snaplet.R
import com.thinh.snaplet.domain.model.RelationshipAction
import com.thinh.snaplet.ui.components.AppIconButton
import com.thinh.snaplet.ui.components.Avatar
import com.thinh.snaplet.ui.components.BaseText
import com.thinh.snaplet.ui.components.IconSpec
import com.thinh.snaplet.ui.components.PrimaryButton
import com.thinh.snaplet.ui.screens.friend_request.FriendRequestUiState

private val ICON_SIZE = 56.dp

@Composable
internal fun UserProfileCard(
    modifier: Modifier = Modifier,
    state: FriendRequestUiState,
    onSendRequest: () -> Unit,
    onRefreshPending: () -> Unit = {},
    onAcceptRequest: () -> Unit = {},
) {
    val shape = RoundedCornerShape(24.dp)

    Column(
        modifier = modifier
//            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh, shape)
            .border(2.dp, MaterialTheme.colorScheme.surfaceVariant, shape)
            .padding(top = 60.dp, bottom = 40.dp, start = 24.dp, end = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Avatar(
            avatarUrl = state.userProfile?.avatarUrls?.forMedium().orEmpty(),
            firstName = state.userProfile?.firstName.orEmpty(),
            size = 120.dp,
            isConnectedUser = false
        )

        Spacer(modifier = Modifier.height(52.dp))

        BaseText(
            text = state.userProfile?.displayName.orEmpty(),
            typography = MaterialTheme.typography.headlineSmall,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(20.dp))

        Box(
            modifier = Modifier.height(50.dp), contentAlignment = Alignment.Center
        ) {
            when {
                state.isCurrentUser -> {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.secondary, shape = CircleShape)
                            .width(ICON_SIZE), contentAlignment = Alignment.Center
                    ) {
                        BaseText(
                            text = stringResource(R.string.you),
                            typography = MaterialTheme.typography.bodyLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                state.relationshipAction == RelationshipAction.Accepted -> {
                    PrimaryButton(
                        modifier = Modifier.fillMaxHeight(),
                        onClick = {},
                        title = stringResource(R.string.friends),
                        typography = MaterialTheme.typography.bodyLarge,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        titleColor = Color.Black,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                modifier = Modifier.size(28.dp),
                                tint = Color.Black
                            )
                        })
                }

                state.relationshipAction == RelationshipAction.Blocked -> {
                    PrimaryButton(
                        modifier = Modifier.fillMaxHeight(),
                        onClick = {},
                        title = stringResource(R.string.blocked),
                        typography = MaterialTheme.typography.bodyLarge,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        titleColor = Color.White,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Block,
                                contentDescription = null,
                                modifier = Modifier.size(28.dp),
                                tint = Color.White
                            )
                        })
                }

                state.relationshipAction is RelationshipAction.PendingByMe -> {
                    AppIconButton(
                        modifier = Modifier.size(ICON_SIZE),
                        icon = IconSpec.Vector(
                            Icons.Outlined.Refresh, tint = MaterialTheme.colorScheme.primary
                        ),
                        loading = state.isRequesting,
                        onClick = onRefreshPending,
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.primary,
                        iconSize = 28.dp
                    )
                }

                state.relationshipAction is RelationshipAction.PendingByOther -> {
                    PrimaryButton(
                        modifier = Modifier.fillMaxHeight(),
                        onClick = onAcceptRequest,
                        title = stringResource(R.string.accept),
                        typography = MaterialTheme.typography.bodyLarge,
                        isLoading = state.isRequesting,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        titleColor = Color.Black,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                modifier = Modifier.size(28.dp),
                                tint = Color.Black
                            )
                        })
                }

                else -> {
                    PrimaryButton(
                        modifier = Modifier.fillMaxHeight(),
                        onClick = onSendRequest,
                        title = stringResource(R.string.add_friend),
                        typography = MaterialTheme.typography.bodyLarge,
                        isLoading = state.isRequesting,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        titleColor = Color.Black,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = null,
                                modifier = Modifier.size(28.dp),
                                tint = Color.Black
                            )
                        })
                }
            }
        }
    }
}