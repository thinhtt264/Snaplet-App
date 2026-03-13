package com.thinh.snaplet.ui.overlay.modal

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.thinh.snaplet.R
import com.thinh.snaplet.ui.common.UiText
import com.thinh.snaplet.ui.components.BaseText
import com.thinh.snaplet.ui.overlay.ModalContent

@Composable
internal fun ConfirmDialog(
    content: ModalContent.ConfirmDialog,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val typography = MaterialTheme.typography
    val colorScheme = MaterialTheme.colorScheme
    val cancelText = content.cancelText
        ?: UiText.StringResource(R.string.cancel)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            BaseText(
                text = content.title.asString(context),
                typography = typography.headlineSmall,
                color = colorScheme.onSurface
            )
        },
        text = {
            BaseText(
                text = content.message.asString(context),
                typography = typography.bodyMedium,
                color = colorScheme.onSurface
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                BaseText(
                    text = cancelText.asString(context),
                    typography = typography.titleSmall,
                    color = if (content.confirmDestructive) colorScheme.primary else colorScheme.onSurface
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    content.onConfirm()
                    onDismiss()
                }
            ) {
                BaseText(
                    text = content.confirmText.asString(context),
                    typography = typography.titleSmall,
                    color = if (content.confirmDestructive) colorScheme.error else colorScheme.primary
                )
            }
        }
    )
}