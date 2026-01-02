package com.thinh.snaplet.ui.screens.login

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import animateVisibility
import com.thinh.snaplet.R
import com.thinh.snaplet.ui.components.AppText
import com.thinh.snaplet.ui.components.FormTextField
import com.thinh.snaplet.ui.components.PrimaryButton

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Login(
    viewModel: LoginViewModel = hiltViewModel(),
    onLoginSuccess: () -> Unit,
    onRegisterClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current
    val pagerState = rememberPagerState(pageCount = { 2 })

    LaunchedEffect(uiState.currentStep) {
        val targetPage = if (uiState.currentStep == LoginStep.EMAIL) 0 else 1
        if (pagerState.currentPage != targetPage) {
            pagerState.animateScrollToPage(targetPage)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .imePadding()
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

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            userScrollEnabled = false
        ) { page ->
            Box(Modifier.fillMaxSize()) {
                when (page) {
                    0 -> EmailPage(
                        email = uiState.email,
                        emailError = uiState.emailError,
                        isLoading = uiState.isLoading,
                        onEmailChange = viewModel::onEmailChange,
                        onContinue = viewModel::onContinueFromEmail,
                        focusManager = focusManager,
                        onRegisterClick = onRegisterClick
                    )

                    1 -> PasswordPage(
                        email = uiState.email,
                        password = uiState.password,
                        passwordError = uiState.passwordError,
                        isPasswordVisible = uiState.isPasswordVisible,
                        errorMessage = uiState.errorMessage,
                        isLoading = uiState.isLoading,
                        onPasswordChange = viewModel::onPasswordChange,
                        onPasswordVisibilityToggle = viewModel::onPasswordVisibilityToggle,
                        onLogin = { viewModel.onLogin(onLoginSuccess) },
                        focusManager = focusManager
                    )
                }
            }
        }
    }
}

@Composable
private fun LoginPageContent(
    title: String,
    subtitle: String,
    buttonText: String,
    isLoading: Boolean,
    onButtonClick: () -> Unit,
    inputField: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    subtitleColor: Color = colorScheme.secondary,
    extraContent: @Composable (() -> Unit)? = null,
    errorContent: @Composable (() -> Unit)? = null,
    bottomContent: @Composable (() -> Unit)? = null
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.height(96.dp)
        ) {
            AppText(
                text = title, typography = typography.displaySmall, color = colorScheme.onBackground
            )

            Spacer(Modifier.height(12.dp))

            AppText(
                text = subtitle, typography = typography.bodyLarge, color = subtitleColor
            )
        }

        Spacer(Modifier.height(48.dp))

        inputField()

        Box(
            modifier = Modifier.height(52.dp), contentAlignment = Alignment.TopStart
        ) {
            Column {
                extraContent?.invoke()
            }
        }

        PrimaryButton(
            onClick = onButtonClick,
            title = if (isLoading) "" else buttonText,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 18.dp),
            enabled = !isLoading,
            colors = ButtonDefaults.buttonColors(
                containerColor = colorScheme.primary,
                disabledContainerColor = colorScheme.primary.copy(alpha = 0.5f)
            ),
            titleColor = Color.Black
        )

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp), color = Color.Black, strokeWidth = 2.dp
                )
            }
        } else {
            Spacer(Modifier.height(42.dp))
        }

        errorContent?.invoke()

        bottomContent?.invoke()

        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun EmailPage(
    email: String,
    emailError: String?,
    isLoading: Boolean,
    onEmailChange: (String) -> Unit,
    onContinue: () -> Unit,
    focusManager: FocusManager,
    onRegisterClick: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    LoginPageContent(
        title = stringResource(R.string.login_email_title),
        subtitle = stringResource(R.string.login_subtitle),
        buttonText = stringResource(R.string.continue_text),
        isLoading = isLoading,
        onButtonClick = onContinue,
        inputField = {
            FormTextField(
                value = email,
                onValueChange = onEmailChange,
                label = stringResource(R.string.email),
                placeholder = stringResource(R.string.email_placeholder),
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Done,
                onImeAction = {
                    focusManager.clearFocus()
                    onContinue()
                },
                errorMessage = emailError,
                enabled = !isLoading,
                modifier = Modifier.focusRequester(focusRequester)
            )
        },
        bottomContent = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppText(
                    text = stringResource(R.string.dont_have_account),
                    typography = typography.bodyMedium,
                    color = colorScheme.secondary
                )
                Spacer(Modifier.size(4.dp))
                AppText(
                    text = stringResource(R.string.sign_up),
                    typography = typography.bodyMedium,
                    color = colorScheme.primary,
                    modifier = Modifier.clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }) {
                        onRegisterClick()
                    })
            }
        })
}

@Composable
private fun PasswordPage(
    email: String,
    password: String,
    passwordError: String?,
    isPasswordVisible: Boolean,
    errorMessage: String?,
    isLoading: Boolean,
    onPasswordChange: (String) -> Unit,
    onPasswordVisibilityToggle: () -> Unit,
    onLogin: () -> Unit,
    focusManager: FocusManager
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    LoginPageContent(
        title = stringResource(R.string.login_password_title),
        subtitle = email,
        subtitleColor = colorScheme.primary,
        buttonText = stringResource(R.string.login),
        isLoading = isLoading,
        onButtonClick = onLogin,
        inputField = {
            FormTextField(
                value = password,
                onValueChange = onPasswordChange,
                label = stringResource(R.string.password),
                placeholder = stringResource(R.string.password_placeholder),
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
                onImeAction = {
                    focusManager.clearFocus()
                    onLogin()
                },
                errorMessage = passwordError,
                enabled = !isLoading,
                visualTransformation = if (isPasswordVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                trailingIcon = {
                    IconButton(
                        onClick = onPasswordVisibilityToggle, enabled = !isLoading
                    ) {
                        Icon(
                            imageVector = if (isPasswordVisible) {
                                Icons.Filled.VisibilityOff
                            } else {
                                Icons.Filled.Visibility
                            },
                            contentDescription = "Toggle Password Visibility",
                            tint = colorScheme.secondary
                        )
                    }
                },
                modifier = Modifier.focusRequester(focusRequester)
            )
        },
        extraContent = {
            AnimatedVisibility(
                visible = password.isNotEmpty() && password.length < 8,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                AppText(
                    text = stringResource(R.string.password_requirement),
                    typography = typography.bodySmall,
                    color = colorScheme.onError,
                    modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                )
            }

            Spacer(Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End
            ) {
                AppText(
                    text = stringResource(R.string.forgot_password),
                    typography = typography.bodySmall,
                    color = colorScheme.primary,
                    modifier = Modifier.clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }) {
                        // TODO: Navigate to forgot password screen
                    })
            }
        },
        errorContent = {
            AnimatedVisibility(
                visible = errorMessage != null, enter = fadeIn(), exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = colorScheme.onError, shape = RoundedCornerShape(12.dp)
                        )
                        .padding(16.dp)
                ) {
                    AppText(
                        text = errorMessage ?: "",
                        typography = typography.bodyMedium,
                        color = colorScheme.onError,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        })
}