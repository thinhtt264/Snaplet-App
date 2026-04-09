package com.thinh.snaplet.ui.screens.home.components

import android.os.SystemClock
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.thinh.snaplet.R
import com.thinh.snaplet.ui.common.CommonImages
import com.thinh.snaplet.ui.components.AvatarGroup
import com.thinh.snaplet.ui.components.AvatarGroupItem
import com.thinh.snaplet.ui.components.BaseText
import com.thinh.snaplet.ui.screens.home.PostReactionsUiState
import com.thinh.snaplet.ui.theme.MotionTokens
import com.thinh.snaplet.utils.ensureMinLoadingTime
import pressScaleClickable

@Composable
fun PostActivityBar(
    modifier: Modifier = Modifier,
    model: PostActivityBarModel = PostActivityBarModel(),
) {
    var displayedState by remember { mutableStateOf(model.state) }
    var loadingStartTimeMillis by remember {
        mutableLongStateOf(
            if (model.state is PostReactionsUiState.Loading) SystemClock.elapsedRealtime() else 0L
        )
    }

    // Ensure the `Loading` UI stays visible for at least the configured minimum time,
    LaunchedEffect(model.state) {
        val targetState = model.state
        val currentDisplayedState = displayedState

        if (targetState is PostReactionsUiState.Loading) {
            if (currentDisplayedState !is PostReactionsUiState.Loading) {
                loadingStartTimeMillis = SystemClock.elapsedRealtime()
            }
            displayedState = targetState
            return@LaunchedEffect
        }

        if (currentDisplayedState is PostReactionsUiState.Loading) {
            val start = loadingStartTimeMillis
            if (start > 0L) ensureMinLoadingTime(start)
        }
        displayedState = targetState
    }

    val isClickable: Boolean =
        (displayedState as? PostReactionsUiState.Result)?.reactions?.isNotEmpty() == true

    AnimatedContent(
        targetState = displayedState,
        transitionSpec = {
            fadeIn(animationSpec = tween(durationMillis = MotionTokens.Slow)).togetherWith(
                fadeOut(
                    animationSpec = tween(durationMillis = MotionTokens.Slow)
                )
            )
        },
        modifier = modifier,
        label = "PostActivityBar",
    ) { currentState ->
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(24.dp))
                .background(
                    MaterialTheme.colorScheme.surfaceContainerHighest,
                    shape = RoundedCornerShape(24.dp)
                )
                .height(48.dp)
                .widthIn(max = 260.dp)
                .padding(horizontal = 12.dp)
                .animateContentSize()
                .pressScaleClickable(enabled = isClickable, onClick = model.onClick),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally)
        ) {
            Icon(
                painter = painterResource(CommonImages.SparkleIcon),
                contentDescription = null,
                modifier = Modifier
                    .size(24.dp)
                    .padding(bottom = 2.dp),
                tint = MaterialTheme.colorScheme.onSurface,
            )

            when (currentState) {
                is PostReactionsUiState.Loading -> {
                    BaseText(
                        text = stringResource(R.string.post_activity_bar_loading),
                        typography = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.width(2.dp))
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        color = MaterialTheme.colorScheme.onSurface,
                        strokeWidth = 1.5.dp,
                    )
                }

                is PostReactionsUiState.Result -> {
                    if (currentState.reactions.isEmpty()) {
                        BaseText(
                            text = stringResource(R.string.post_activity_bar_empty),
                            typography = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val reactionCount = currentState.reactions.size
                            BaseText(
                                text = pluralStringResource(
                                    R.plurals.post_activity_bar_count,
                                    reactionCount,
                                    reactionCount,
                                ),
                                typography = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            AvatarGroup(
                                items = currentState.reactions.map {
                                    AvatarGroupItem(
                                        avatarUrl = it.avatarUrl.ifBlank { null },
                                        firstName = it.firstName,
                                    )
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}
