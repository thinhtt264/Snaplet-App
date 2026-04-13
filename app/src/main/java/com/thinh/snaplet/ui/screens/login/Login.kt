package com.thinh.snaplet.ui.screens.login

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.thinh.snaplet.ui.components.BaseText
import com.thinh.snaplet.ui.components.StepAnimatedContent
import com.thinh.snaplet.ui.screens.login.components.LoginEmailPage
import com.thinh.snaplet.ui.screens.login.components.LoginPasswordPage

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Login(
    viewModel: LoginViewModel = hiltViewModel(), onRegisterClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current

    var errorDialogMessage by rememberSaveable {
        mutableStateOf<String?>(null)
    }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is LoginUIEvent.LoginSuccess -> {}
                is LoginUIEvent.NavigateToRegister -> onRegisterClick()
                is LoginUIEvent.ShowErrorPopup -> {
                    errorDialogMessage = event.message.asString(context)
                }
            }
        }
    }

    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .imePadding()
            .verticalScroll(scrollState)
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    focusManager.clearFocus()
                })
            }) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
        ) {
            IconButton(
                onClick = viewModel::onBackToEmailStep,
                enabled = !uiState.isLoading,
                modifier = Modifier
                    .padding(16.dp)
                    .size(48.dp)
                    .animateVisibility(uiState.currentStep == LoginStep.PASSWORD)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = colorScheme.onBackground
                )
            }
        }

        StepAnimatedContent(
            currentStep = uiState.currentStep,
            stepOrder = { step ->
                when (step) {
                    LoginStep.EMAIL -> 0
                    LoginStep.PASSWORD -> 1
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
        ) { step ->
            when (step) {
                LoginStep.EMAIL -> LoginEmailPage(
                    email = uiState.email,
                    emailError = uiState.emailError?.asString(context),
                    isLoading = uiState.isLoading,
                    onEmailChange = viewModel::onEmailChange,
                    onContinue = viewModel::onContinueFromEmail,
                    focusManager = focusManager,
                    onRegisterClick = viewModel::onNavigateToRegister
                )

                LoginStep.PASSWORD -> LoginPasswordPage(
                    email = uiState.email,
                    password = uiState.password,
                    passwordError = uiState.passwordError?.asString(context),
                    isPasswordVisible = uiState.isPasswordVisible,
                    isLoading = uiState.isLoading,
                    onPasswordChange = viewModel::onPasswordChange,
                    onPasswordVisibilityToggle = viewModel::onPasswordVisibilityToggle,
                    onLogin = viewModel::onLogin,
                    focusManager = focusManager
                )
            }
        }
    }

    // Error Alert Dialog
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