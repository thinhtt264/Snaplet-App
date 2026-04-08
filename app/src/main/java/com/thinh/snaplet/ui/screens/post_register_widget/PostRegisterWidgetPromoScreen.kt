package com.thinh.snaplet.ui.screens.post_register_widget

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowCircleDown
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.thinh.snaplet.R
import com.thinh.snaplet.ui.common.CommonImages
import com.thinh.snaplet.ui.components.BaseText
import com.thinh.snaplet.ui.components.PrimaryButton

@Composable
fun PostRegisterWidgetPromoScreen(
    onAddWidget: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    val scroll = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(CommonImages.WidgetIllustration),
            contentDescription = stringResource(R.string.post_register_widget_illustration_cd),
            modifier = Modifier.fillMaxWidth(),
            contentScale = ContentScale.FillWidth,
        )

        BaseText(
            text = stringResource(R.string.post_register_widget_title_line1),
            typography = typography.headlineMedium,
            color = colorScheme.onBackground,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        BaseText(
            text = stringResource(R.string.post_register_widget_title_line2),
            typography = typography.headlineMedium,
            color = colorScheme.primary,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 16.dp),
        )

        WidgetPromoFeatureCard(
            icon = Icons.Outlined.GridView,
            title = stringResource(R.string.post_register_widget_feature1_title),
            body = stringResource(R.string.post_register_widget_feature1_body),
        )
        Spacer(Modifier.height(12.dp))
        WidgetPromoFeatureCard(
            icon = Icons.Outlined.ArrowCircleDown,
            title = stringResource(R.string.post_register_widget_feature2_title),
            body = stringResource(R.string.post_register_widget_feature2_body),
        )

        Spacer(Modifier.height(24.dp))

        PrimaryButton(
            onClick = onAddWidget,
            title = stringResource(R.string.post_register_widget_cta),
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = colorScheme.primary,
                contentColor = colorScheme.onPrimary,
                disabledContainerColor = colorScheme.surfaceContainerHighest,
                disabledContentColor = colorScheme.onSurfaceVariant,
            ),
            contentPadding = PaddingValues(vertical = 12.dp),
            titleColor = colorScheme.background,
            typography = typography.titleMedium,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.Widgets,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = colorScheme.onPrimary,
                )
            },
        )

        TextButton(
            onClick = onSkip,
            modifier = Modifier.padding(top = 4.dp),
        ) {
            BaseText(
                text = stringResource(R.string.post_register_widget_later),
                typography = typography.bodyMedium,
                color = colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun WidgetPromoFeatureCard(
    icon: ImageVector,
    title: String,
    body: String,
) {
    val colorScheme = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = colorScheme.surfaceContainerHigh,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(12.dp),
                color = colorScheme.surfaceContainerHighest,
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = colorScheme.primary,
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                BaseText(
                    text = title,
                    typography = typography.titleSmall,
                    color = colorScheme.onBackground,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(4.dp))
                BaseText(
                    text = body,
                    typography = typography.bodyMedium,
                    color = colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
