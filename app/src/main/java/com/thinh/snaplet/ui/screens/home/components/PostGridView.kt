package com.thinh.snaplet.ui.screens.home.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.thinh.snaplet.data.model.post.Post
import com.thinh.snaplet.ui.components.image.AsyncImage
import com.thinh.snaplet.ui.components.image.ImageSize
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map

@Composable
fun PostGridView(
    modifier: Modifier = Modifier,
    posts: List<Post>,
    onItemClick: (index: Int) -> Unit,
    onLoadMore: () -> Unit,
    canLoadMore: Boolean,
    onCaptureClick: () -> Unit,
    loadMoreTriggerFromBottomRatio: Float = 0.3f,
) {
    val gridState = rememberLazyGridState()

    LaunchedEffect(gridState, posts.size, canLoadMore, loadMoreTriggerFromBottomRatio) {
        if (!canLoadMore || posts.isEmpty()) return@LaunchedEffect

        snapshotFlow {
            val lastVisibleIndex = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            val totalCount = posts.size
            lastVisibleIndex to totalCount
        }
            .map { (lastVisibleIndex, totalCount) ->
                val triggerIndex = ((totalCount - 1) * (1f - loadMoreTriggerFromBottomRatio))
                    .toInt()
                    .coerceAtLeast(0)
                lastVisibleIndex >= triggerIndex
            }
            .distinctUntilChanged()
            .filter { it }
            .collectLatest { onLoadMore() }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            state = gridState,
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            contentPadding = PaddingValues(
                start = 2.dp,
                top = 2.dp,
                end = 2.dp,
                // Keep space for the capture button overlay at bottom-center.
                bottom = 100.dp,
            ),
        ) {
            itemsIndexed(posts, key = { _, post -> post.id }) { index, post ->
                PostGridItem(
                    post = post,
                    onClick = { onItemClick(index) },
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
                .navigationBarsPadding()
                .align(Alignment.BottomCenter),
            contentAlignment = Alignment.Center,
        ) {
            CaptureButton(
                modifier = Modifier.size(56.dp),
                onCapturePhoto = onCaptureClick,
            )
        }
    }
}

@Composable
fun PostGridItem(
    post: Post,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(4.dp))
            .clickable(onClick = onClick),
    ) {
        val media = post.media.firstOrNull()
        if (media != null) {
            AsyncImage(
                imageUrl = media.images.md,
                contentDescription = "Post ${post.id}",
                contentScale = ContentScale.Crop,
                resizeSize = ImageSize.Small,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
