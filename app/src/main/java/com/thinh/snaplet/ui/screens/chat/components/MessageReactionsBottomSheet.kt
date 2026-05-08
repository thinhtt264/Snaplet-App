package com.thinh.snaplet.ui.screens.chat.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thinh.snaplet.R
import com.thinh.snaplet.data.model.chat.MessageReactionWithUserInfo
import com.thinh.snaplet.ui.components.Avatar
import com.thinh.snaplet.ui.components.BaseText
import pressScaleClickable

private val REACTIONS_SHEET_AVATAR_SIZE = 40.dp
private val REACTIONS_SHEET_EMOJI_FONT_SIZE = 24.sp
private val REACTIONS_SHEET_ROW_SPACING = 12.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageReactionsBottomSheet(
    reactions: List<MessageReactionWithUserInfo>,
    isLoading: Boolean,
    error: String?,
    currentUserId: String?,
    onDismiss: () -> Unit,
    onMyReactionClick: (emoji: String) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .navigationBarsPadding()
                .padding(all = 16.dp),
        ) {
            BaseText(
                text = stringResource(R.string.reactions_sheet_title),
                typography = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 16.dp),
            )

            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(vertical = 24.dp),
                    )
                }

                error != null -> {
                    BaseText(
                        text = error,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                        typography = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }

                else -> {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        items(reactions, key = { it.userId }) { reaction ->
                            ReactionReactorRow(
                                reaction = reaction,
                                currentUserId = currentUserId,
                                onMyReactionClick = onMyReactionClick,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReactionReactorRow(
    reaction: MessageReactionWithUserInfo,
    currentUserId: String?,
    onMyReactionClick: (emoji: String) -> Unit,
) {
    val isMine = reaction.userId == currentUserId
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pressScaleClickable(enabled = isMine) { onMyReactionClick(reaction.emoji) },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(REACTIONS_SHEET_ROW_SPACING),
    ) {
        Avatar(
            avatarUrl = reaction.user.avatarUrls.forThumbnail(),
            firstName = reaction.user.firstName,
            size = REACTIONS_SHEET_AVATAR_SIZE,
            isConnectedUser = false,
        )

        Column(modifier = Modifier.weight(1f)) {
            BaseText(
                text = reaction.user.displayName,
                typography = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            if (isMine) {
                BaseText(
                    text = stringResource(R.string.chat_reaction_unreact_hint),
                    typography = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }

        Spacer(Modifier.width(4.dp))

        BaseText(
            text = reaction.emoji,
            typography = MaterialTheme.typography.bodyMedium.copy(fontSize = REACTIONS_SHEET_EMOJI_FONT_SIZE),
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Clip,
        )
    }
}

