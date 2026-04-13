package com.thinh.snaplet.ui.screens.onboarding

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.LocaleListCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.thinh.snaplet.R
import com.thinh.snaplet.ui.common.CommonImages
import com.thinh.snaplet.ui.components.BaseText
import com.thinh.snaplet.ui.components.PrimaryButton
import com.thinh.snaplet.utils.Logger

@Composable
fun Onboarding(
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel = hiltViewModel(),
    onNavigateToLogin: () -> Unit,
    onNavigateToRegister: (String?, String?) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is OnboardingUiEvent.NavigateToRegister -> {
                    onNavigateToRegister(event.firstName, event.lastName)
                }
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier.weight(0.7f)
            ) {
                BaseText(
                    text = stringResource(R.string.app_name),
                    typography = typography.headlineLarge,
                    fontSize = 52.sp,
                    lineHeight = 40.sp
                )
                Spacer(Modifier.height(20.dp))
                BaseText(
                    color = colorScheme.onSurface,
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
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onNavigateToRegister(null, null) },
                    enabled = !uiState.isLoading,
                    title = stringResource(R.string.create_account),
                    contentPadding = PaddingValues(vertical = 16.dp, horizontal = 18.dp),
                    titleColor = Color.Black,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorScheme.primary,
                        disabledContainerColor = colorScheme.primary.copy(0.6f)
                    ),
                )
                Spacer(Modifier.height(16.dp))
                PrimaryButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { viewModel.loginWithGoogle(context) },
                    isLoading = uiState.isLoading,
                    title = stringResource(R.string.continue_with_google),
                    contentPadding = PaddingValues(vertical = 16.dp, horizontal = 18.dp),
                    titleColor = Color.White,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    border = BorderStroke(1.dp, colorScheme.outline.copy(alpha = 0.5f)),
                    leadingIcon = {
                        Icon(
                            painter = painterResource(CommonImages.GoogleLogo),
                            contentDescription = "Google logo",
                            modifier = Modifier
                                .size(30.dp)
                                .padding(end = 8.dp),
                            tint = Color.Unspecified
                        )
                    })
                Spacer(Modifier.height(16.dp))
                PrimaryButton(
                    onClick = onNavigateToLogin,
                    enabled = !uiState.isLoading,
                    title = stringResource(R.string.login)
                )
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(vertical = 24.dp), horizontalArrangement = Arrangement.Center
        ) {
            OutlinedButton(
                contentPadding = PaddingValues(vertical = 8.dp, horizontal = 16.dp),
                onClick = { changeLocale("vi") }) {
                BaseText("🇻🇳 Tiếng Việt")
            }
            Spacer(modifier = Modifier.width(12.dp))
            OutlinedButton(
                contentPadding = PaddingValues(vertical = 12.dp, horizontal = 24.dp),
                onClick = { changeLocale("en") }) {
                BaseText("🇺🇸 English")
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