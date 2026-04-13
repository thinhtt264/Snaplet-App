package com.thinh.snaplet.ui.screens.login.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.thinh.snaplet.ui.components.BaseText
import com.thinh.snaplet.ui.components.PrimaryButton

@Composable
fun LoginPageContent(
    title: String,
    subtitle: String,
    buttonText: String,
    buttonEnabled: Boolean,
    isLoading: Boolean,
    onButtonClick: () -> Unit,
    inputField: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    subtitleColor: Color = colorScheme.onSurface,
    extraContent: @Composable (() -> Unit)? = null,
    bottomContent: @Composable (() -> Unit)? = null
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier.height(96.dp)
        ) {
            BaseText(
                text = title,
                typography = typography.displaySmall,
                color = colorScheme.onBackground
            )

            Spacer(Modifier.height(12.dp))

            BaseText(
                text = subtitle,
                typography = typography.bodyLarge,
                color = subtitleColor
            )

        }

        Spacer(Modifier.height(40.dp))

        inputField()

        Box(
            modifier = Modifier.height(40.dp),
            contentAlignment = Alignment.TopStart
        ) {
            Column {
                extraContent?.invoke()
            }
        }

        PrimaryButton(
            onClick = onButtonClick,
            title = buttonText,
            modifier = Modifier.width(220.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            enabled = buttonEnabled,
            isLoading = isLoading,
            colors = ButtonDefaults.buttonColors(
                containerColor = colorScheme.primary,
                disabledContainerColor = colorScheme.primary.copy(0.7f)
            ),
            titleColor = Color.Black
        )

        bottomContent?.invoke()

        Spacer(Modifier.height(40.dp))
    }
}