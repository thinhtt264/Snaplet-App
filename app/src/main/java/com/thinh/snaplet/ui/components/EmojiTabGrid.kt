package com.thinh.snaplet.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thinh.snaplet.data.model.emoji.EmojiEntry
import com.thinh.snaplet.data.model.emoji.EmojiTab

@Composable
fun EmojiTabGrid(
    groupedEmojis: Map<EmojiTab, List<EmojiEntry>>,
    modifier: Modifier = Modifier,
    columns: Int = 8,
    onEmojiClick: (EmojiEntry) -> Unit,
) {
    val tabs = remember(groupedEmojis) { groupedEmojis.keys.toList() }
    var selectedTab by remember(tabs) { mutableIntStateOf(0) }
    val selected = tabs.getOrNull(selectedTab)
    val displayEmojis = selected?.let { groupedEmojis[it] }.orEmpty()

    androidx.compose.foundation.layout.Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        PrimaryScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurface,
            edgePadding = 4.dp,
            divider = {},
        ) {
            tabs.forEachIndexed { index, tab ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(text = tab.icon, fontSize = 20.sp) },
                )
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            items(displayEmojis, key = { it.hexcode ?: it.unicode }) { entry ->
                Text(
                    text = entry.unicode,
                    fontSize = 28.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .clickable { onEmojiClick(entry) }
                        .padding(6.dp),
                )
            }
        }
    }
}
