package com.thinh.snaplet.ui.screens.home.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.thinh.snaplet.ui.common.UiText
import com.thinh.snaplet.ui.components.BaseText
import com.thinh.snaplet.ui.theme.MotionTokens
import pressScaleClickable

@Composable
fun NewPostsBanner(
    bannerMessage: UiText?,
    isEligiblePage: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val visible = bannerMessage != null && isEligiblePage

    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = slideInVertically(
            animationSpec = tween(durationMillis = MotionTokens.Emphasized),
            initialOffsetY = { -it }
        ) + fadeIn(animationSpec = tween(durationMillis = MotionTokens.Normal)),
        exit = slideOutVertically(
            animationSpec = tween(durationMillis = MotionTokens.Emphasized),
            targetOffsetY = { -it }
        ) + fadeOut(animationSpec = tween(durationMillis = MotionTokens.Fast)),
    ) {
        Surface(
            modifier = Modifier.pressScaleClickable(onClick = onClick),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
        ) {
            Row(
                modifier = Modifier.padding(vertical = 8.dp, horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowUpward,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onPrimary,
                )
                BaseText(
                    text = bannerMessage?.asString(context).orEmpty(),
                    typography = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }
    }
}