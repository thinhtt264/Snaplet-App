package com.thinh.snaplet.ui.overlay.modal

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.GppBad
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.thinh.snaplet.R
import com.thinh.snaplet.ui.components.BaseText
import com.thinh.snaplet.ui.components.PrimaryButton
import com.thinh.snaplet.ui.overlay.ModalContent

@Composable
internal fun ForceLogoutModal(
    content: ModalContent.ForceLogoutDialog,
    onDismiss: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    val cardRadius = 24.dp
    val iconWrapSize = 52.dp
    val iconCornerRadius = 16.dp

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(colorScheme.surfaceContainer, RoundedCornerShape(cardRadius))
                .border(
                    BorderStroke(0.5.dp, colorScheme.outlineVariant),
                    RoundedCornerShape(cardRadius),
                ),
            contentAlignment = Alignment.Center,
        ) {
            // Ambient glow (dialog::before)
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .size(220.dp, 180.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                colorScheme.errorContainer.copy(alpha = 0.18f),
                                Color.Transparent,
                            ),
                        ),
                    ),
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 28.dp, horizontal = 16.dp)
            ) {
                Box(
                    modifier = Modifier.size(iconWrapSize),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                                colorScheme.errorContainer, RoundedCornerShape(iconCornerRadius)
                            )
                            .border(
                                BorderStroke(
                                    1.dp, colorScheme.onErrorContainer.copy(alpha = 0.2f)
                                ),
                                RoundedCornerShape(iconCornerRadius),
                            ),
                    )

                    Icon(
                        modifier = Modifier.size(32.dp),
                        imageVector = Icons.Filled.GppBad,
                        contentDescription = "Session expired",
                        tint = colorScheme.onErrorContainer,
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                BaseText(
                    text = stringResource(R.string.force_logout_title),
                    typography = typography.titleLarge,
                    color = colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(16.dp))

                BaseText(
                    text = stringResource(R.string.force_logout_message),
                    typography = typography.bodySmall,
                    color = colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(40.dp))

                PrimaryButton(
                    onClick = {
                        content.onConfirm()
                        onDismiss()
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.Logout,
                            tint = colorScheme.onPrimary,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                    },
                    shape = CircleShape,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 12.dp),
                    title = stringResource(R.string.force_logout_button),
                    titleColor = colorScheme.onPrimary,
                    typography = typography.titleSmall,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorScheme.primary,
                        contentColor = colorScheme.onPrimary,
                    ),
                )
            }
        }
    }
}