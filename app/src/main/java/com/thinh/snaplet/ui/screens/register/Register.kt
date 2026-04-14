package com.thinh.snaplet.ui.screens.register

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import animateVisibility
import com.thinh.snaplet.R
import com.thinh.snaplet.platform.widget.launchSnapletWidgetPicker
import com.thinh.snaplet.ui.components.BaseText
import com.thinh.snaplet.ui.components.StepAnimatedContent
import com.thinh.snaplet.ui.screens.post_register_widget.PostRegisterWidgetPromoScreen
import com.thinh.snaplet.ui.screens.register.components.RegisterEmailPage
import com.thinh.snaplet.ui.screens.register.components.RegisterPasswordPage
import com.thinh.snaplet.ui.screens.register.components.RegisterUsernamePage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Register(
    onLoginClick: () -> Unit, viewModel: RegisterViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var errorDialogMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is RegisterUIEvent.NavigateToLogin -> onLoginClick()
                is RegisterUIEvent.ShowErrorPopup -> {
                    errorDialogMessage = event.message
                }
            }
        }
    }

    fun onGoBack() {
        when (uiState.currentStep) {
            RegisterStep.USERNAME -> viewModel.onScrollToPage(RegisterStep.EMAIL)
            RegisterStep.PASSWORD -> viewModel.onScrollToPage(RegisterStep.USERNAME)
            RegisterStep.EMAIL -> {}
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .imePadding()
            .navigationBarsPadding()
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    focusManager.clearFocus()
                })
            }) {
        if (uiState.showPostRegisterWidgetPromo) {
            PostRegisterWidgetPromoScreen(
                modifier = Modifier.animateVisibility(true),
                onAddWidget = {
                    context.launchSnapletWidgetPicker {
                        scope.launch {
                            delay(500L)
                            viewModel.finishRegistrationPromo()
                        }
                    }
                },
                onSkip = {
                    scope.launch { viewModel.finishRegistrationPromo() }
                },
            )
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                ) {
                    IconButton(
                        onClick = ::onGoBack,
                        enabled = !uiState.isLoading,
                        modifier = Modifier
                            .padding(16.dp)
                            .size(48.dp)
                            .animateVisibility(uiState.currentStep != RegisterStep.EMAIL)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = colorScheme.onBackground
                        )
                    }
                }

                val scrollState = rememberScrollState()

                StepAnimatedContent(
                    currentStep = uiState.currentStep,
                    stepOrder = { step ->
                        when (step) {
                            RegisterStep.EMAIL -> 0
                            RegisterStep.USERNAME -> 1
                            RegisterStep.PASSWORD -> 2
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(scrollState)
                        .padding(horizontal = 24.dp),
                ) { step ->
                    when (step) {
                        RegisterStep.EMAIL -> RegisterEmailPage(
                            email = uiState.email,
                            emailError = uiState.emailError?.asString(context),
                            isLoading = uiState.isLoading,
                            onEmailChange = viewModel::onEmailChange,
                            onContinue = viewModel::onContinueFromEmail,
                            onLoginClick = viewModel::onNavigateToLogin
                        )

                        RegisterStep.USERNAME -> RegisterUsernamePage(
                            email = uiState.email,
                            username = uiState.username,
                            firstName = uiState.firstName,
                            lastName = uiState.lastName,
                            usernameError = uiState.usernameError?.asString(context),
                            firstNameError = uiState.firstNameError?.asString(context),
                            lastNameError = uiState.lastNameError?.asString(context),
                            isLoading = uiState.isLoading,
                            isGoogleLogin = uiState.isFromGoogleLogin,
                            onUsernameChange = viewModel::onUsernameChange,
                            onFirstNameChange = viewModel::onFirstNameChange,
                            onLastNameChange = viewModel::onLastNameChange,
                            onContinue = viewModel::onContinueFromUsername
                        )

                        RegisterStep.PASSWORD -> RegisterPasswordPage(
                            username = uiState.username,
                            password = uiState.password,
                            passwordError = uiState.passwordError?.asString(context),
                            isPasswordVisible = uiState.isPasswordVisible,
                            isLoading = uiState.isLoading,
                            onPasswordChange = viewModel::onPasswordChange,
                            onPasswordVisibilityToggle = viewModel::onPasswordVisibilityToggle,
                            onRegister = viewModel::onRegister
                        )
                    }
                }
            }
        }
    }

    errorDialogMessage?.let { message ->
        AlertDialog(onDismissRequest = { errorDialogMessage = null }, title = {
            BaseText(
                text = stringResource(R.string.error),
                typography = typography.titleLarge,
                color = colorScheme.onBackground
            )
        }, text = {
            BaseText(
                text = message,
                typography = typography.bodyMedium,
                color = colorScheme.onBackground
            )
        }, confirmButton = {
            TextButton(onClick = { errorDialogMessage = null }) {
                BaseText(
                    text = stringResource(R.string.close),
                    typography = typography.labelLarge,
                    color = colorScheme.primary
                )
            }
        })
    }
}