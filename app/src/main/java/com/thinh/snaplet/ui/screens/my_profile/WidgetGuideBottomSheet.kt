package com.thinh.snaplet.ui.screens.my_profile

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.thinh.snaplet.R
import com.thinh.snaplet.ui.common.CommonImages
import com.thinh.snaplet.ui.components.BaseText

private data class WidgetGuideSlide(
    @DrawableRes val imageRes: Int,
    @StringRes val contentRes: Int,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetGuideBottomSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val slides = remember {
        listOf(
            WidgetGuideSlide(
                imageRes = CommonImages.WidgetGuide1,
                contentRes = R.string.widget_guide_step_1,
            ),
            WidgetGuideSlide(
                imageRes = CommonImages.WidgetGuide2,
                contentRes = R.string.widget_guide_step_2,
            ),
            WidgetGuideSlide(
                imageRes = CommonImages.WidgetGuide3,
                contentRes = R.string.widget_guide_step_3,
            ),
            WidgetGuideSlide(
                imageRes = CommonImages.WidgetGuide4,
                contentRes = R.string.widget_guide_step_4,
            ),
        )
    }
    val pagerState = rememberPagerState(pageCount = { slides.size })

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier.statusBarsPadding(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 16.dp)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BaseText(
                text = stringResource(R.string.widget_guide_title),
                typography = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
            ) { page ->
                val item = slides[page]
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Image(
                        painter = painterResource(id = item.imageRes),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(400.dp),
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(72.dp)
                            .padding(horizontal = 8.dp),
                        contentAlignment = Alignment.TopCenter,
                    ) {
                        BaseText(
                            text = stringResource(item.contentRes),
                            typography = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                repeat(slides.size) { index ->
                    val selected = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(if (selected) 8.dp else 6.dp)
                            .clip(CircleShape)
                            .background(
                                if (selected) {
                                    MaterialTheme.colorScheme.onBackground
                                } else {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                                }
                            ),
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
