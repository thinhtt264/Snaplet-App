package com.thinh.snaplet.ui.screens.friend_request

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.thinh.snaplet.ui.screens.friend_request.components.ActionButtons
import com.thinh.snaplet.ui.screens.friend_request.components.UserProfileCard

@Composable
fun FriendRequestOverlayContent(
    state: FriendRequestUiState,
    modifier: Modifier = Modifier,
    viewModel: FriendRequestViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(state.userProfile) {
        viewModel.loadUser(state.userProfile)
    }

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(
            targetState = state.isLoading, transitionSpec = {
                (fadeIn() + scaleIn() togetherWith fadeOut() + scaleOut())
            }, label = "friend_request_content"
        ) { hasContent ->
            if (hasContent) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    UserProfileCard(
                        modifier = Modifier.fillMaxWidth(),
                        state = uiState,
                        onSendRequest = { viewModel.sendFriendRequest() },
                        onRefreshPending = { viewModel.refreshPending() },
                        onAcceptRequest = { viewModel.acceptFriendRequest() })

                    Spacer(modifier = Modifier.height(28.dp))

                    ActionButtons(onDismiss = { viewModel.dismiss() })
                }
            } else {
                CircularProgressIndicator(modifier = Modifier.size(32.dp))
            }
        }
    }
}