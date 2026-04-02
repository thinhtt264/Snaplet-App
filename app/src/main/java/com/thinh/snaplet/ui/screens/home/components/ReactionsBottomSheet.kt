package com.thinh.snaplet.ui.screens.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.thinh.snaplet.domain.model.ReactionUserUi
import com.thinh.snaplet.ui.components.Avatar
import com.thinh.snaplet.ui.components.BaseText

private val AVATAR_SIZE = 40.dp
private val ICON_FONT_SIZE = 24.sp
private val ICON_SPACING = 8.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReactionsBottomSheet(
    reactions: List<ReactionUserUi>,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
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

            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                items(reactions, key = { it.userId }) { reaction ->
                    ReactionUserRow(reaction)
                }
            }
        }
    }
}

@Composable
private fun ReactionUserRow(reaction: ReactionUserUi) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Avatar(
            avatarUrl = reaction.avatarUrl,
            firstName = reaction.firstName,
            size = AVATAR_SIZE,
            isConnectedUser = false
        )

        BaseText(
            text = reaction.displayName,
            typography = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )

        Spacer(Modifier.width(4.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(ICON_SPACING)) {
            reaction.reactionIcons.forEach { icon ->
                BaseText(text = icon, fontSize = ICON_FONT_SIZE)
            }
        }
    }
}
