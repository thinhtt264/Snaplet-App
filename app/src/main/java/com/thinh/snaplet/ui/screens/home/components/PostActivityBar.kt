package com.thinh.snaplet.ui.screens.home.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.thinh.snaplet.R
import com.thinh.snaplet.ui.common.CommonImages
import com.thinh.snaplet.ui.components.BaseText
import com.thinh.snaplet.ui.screens.home.PostReactionsUiState
import com.thinh.snaplet.ui.theme.MotionTokens
import pressScaleClickable

@Composable
fun PostActivityBar(
    modifier: Modifier = Modifier,
    model: PostActivityBarModel = PostActivityBarModel(),
) {
    val isClickable =
        model.state is PostReactionsUiState.Result && model.state.reactions.isNotEmpty()

    AnimatedContent(
        targetState = model.state,
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
                .background(
                    MaterialTheme.colorScheme.surfaceContainerHighest,
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(horizontal = 12.dp, vertical = 12.dp)
                .animateContentSize()
                .pressScaleClickable(enabled = isClickable, onClick = model.onClick),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
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
                    CircularProgressIndicator(
                        modifier = Modifier.size(13.dp),
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
                        BaseText(
                            text = "${currentState.reactions.size} hoạt động",
                            typography = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }
    }
}
