package com.thinh.snaplet.ui.screens.friend_request

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.thinh.snaplet.ui.screens.friend_request.components.ActionButtons
import com.thinh.snaplet.ui.screens.friend_request.components.UserProfileCard

@Composable
fun FriendRequestOverlayScreen(
    modifier: Modifier = Modifier,
    viewModel: FriendRequestViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    when (val state = uiState) {
        is FriendRequestUiState.Visible -> {
            // Only show overlay when user profile is successfully loaded
            FriendRequestOverlayContent(
                isVisible = true,
                state = state,
                onResendRequest = {
                    viewModel.onResendFriendRequest()
                },
                onDismiss = {
                    viewModel.onDismiss()
                },
                modifier = modifier
            )
        }

        is FriendRequestUiState.Loading -> {
            // Silent loading - no UI shown while fetching
            // This provides better UX: user doesn't see loading spinner
        }

        is FriendRequestUiState.Error -> {
            // Silent error - no UI shown if fetch fails
            // Flow ends gracefully without showing anything
        }

        is FriendRequestUiState.Hidden -> {
            // No content when hidden
        }
    }
}

/**
 * FriendRequestOverlayContent - Internal content composable Handles the
 * animated overlay layout
 *
 * @param isVisible Whether overlay is visible
 * @param state Current UI state with user profile data
 * @param onResendRequest Callback for resend action
 * @param onDismiss Callback for dismiss action
 * @param modifier Optional modifier
 */
@Composable
private fun FriendRequestOverlayContent(
    isVisible: Boolean,
    state: FriendRequestUiState.Visible,
    onResendRequest: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.8f)),
            contentAlignment = Alignment.Center
        ) {
            AnimatedVisibility(
                visible = isVisible,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                OverlayCard(
                    state = state,
                    onResendRequest = onResendRequest,
                    onDismiss = onDismiss
                )
            }
        }
    }
}

/**
 * OverlayCard - Internal card layout composable Contains the profile card
 * and action buttons
 *
 * @param state UI state with user profile
 * @param onResendRequest Callback for resend action
 * @param onDismiss Callback for dismiss action
 * @param modifier Optional modifier
 */
@Composable
private fun OverlayCard(
    state: FriendRequestUiState.Visible,
    onResendRequest: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(340.dp)
            .background(
                color = Color(0xFF2D2D2D),
                shape = RoundedCornerShape(24.dp)
            )
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // User Profile Card
        UserProfileCard(
            userProfile = state.userProfile,
            onResendRequest = onResendRequest
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Action Buttons
        ActionButtons(
            onDismiss = onDismiss
        )
    }
}

