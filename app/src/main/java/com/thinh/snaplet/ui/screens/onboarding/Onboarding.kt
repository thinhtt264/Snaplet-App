package com.thinh.snaplet.ui.screens.onboarding

import androidx.appcompat.app.AppCompatDelegate
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
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import com.thinh.snaplet.R
import com.thinh.snaplet.ui.components.AppText
import com.thinh.snaplet.ui.components.PrimaryButton
import com.thinh.snaplet.utils.Logger

@Composable
fun Onboarding(
    modifier: Modifier = Modifier,
    onNavigateToHome: (userName: String) -> Unit = {}
) {
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier.weight(1f)
            ) {
                AppText(
                    text = stringResource(R.string.app_name),
                    typography = typography.displayLarge
                )
                Spacer(Modifier.height(20.dp))
                AppText(
                    color = colorScheme.secondary,
                    typography = typography.headlineSmall,
                    textAlign = TextAlign.Center,
                    text = stringResource(R.string.onboarding_subtitle)
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.weight(1f)
            ) {
                PrimaryButton(
                    onClick = {
                        onNavigateToHome("New User")
                    },
                    title = stringResource(R.string.create_account),
                    contentPadding = PaddingValues(vertical = 14.dp, horizontal = 16.dp),
                    titleColor = Color.Black,
                    colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary)
                )
                Spacer(Modifier.height(6.dp))
                PrimaryButton(
                    onClick = {
                        onNavigateToHome("Existing User")
                    },
                    title = stringResource(R.string.login)
                )
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(24.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            OutlinedButton(
                onClick = { changeLocale("vi") }) {
                AppText("🇻🇳 Tiếng Việt")
            }
            Spacer(modifier = Modifier.width(12.dp))
            OutlinedButton(
                onClick = { changeLocale("en") }) {
                AppText("🇺🇸 English")
            }
        }
    }
}

private fun changeLocale(languageCode: String) {
    try {
        val localeList = LocaleListCompat.forLanguageTags(languageCode)
        AppCompatDelegate.setApplicationLocales(localeList)

        Logger.d("Locale changed successfully to: %s", languageCode)
    } catch (e: Exception) {
        Logger.e(e, "Error changing locale to: %s", languageCode)
    }
}