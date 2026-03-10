package com.thinh.snaplet.ui.overlay.bottom_sheet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.thinh.snaplet.ui.components.BaseText
import com.thinh.snaplet.ui.overlay.BottomSheetContent
import pressScaleClickable

@Composable
internal fun OptionsSheet(
    content: BottomSheetContent.Options,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(28.dp, Alignment.CenterVertically),
    ) {
        if (content.title != null) {
            BaseText(
                text = content.title.asString(context),
                textAlign = TextAlign.Center,
                typography = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 4.dp),
            )
        }

        content.options.forEach { option ->
            BaseText(
                text = option.label.asString(context),
                textAlign = TextAlign.Center,
                typography = MaterialTheme.typography.titleMedium,
                color = option.color ?: Color.Unspecified,
                modifier = Modifier
                    .fillMaxWidth()
                    .pressScaleClickable(
                        onClick = {
                            onDismiss()
                            option.onClick()
                        })
            )
        }
    }
}