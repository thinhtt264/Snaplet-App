package com.thinh.snaplet.ui.screens.login.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.thinh.snaplet.R
import com.thinh.snaplet.ui.components.BaseText
import com.thinh.snaplet.ui.components.FormTextField
import pressScaleClickable

@Composable
fun LoginEmailPage(
    email: String,
    emailError: String?,
    isLoading: Boolean,
    onEmailChange: (String) -> Unit,
    onContinue: () -> Unit,
    focusManager: FocusManager = LocalFocusManager.current,
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
        buttonEnabled = !isLoading && emailError == null,
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
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .semantics { contentType = ContentType.EmailAddress }
            )
        },
        bottomContent = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BaseText(
                    text = stringResource(R.string.dont_have_account),
                    typography = typography.bodyMedium,
                    color = colorScheme.onSurface
                )
                Spacer(Modifier.size(4.dp))
                BaseText(
                    text = stringResource(R.string.sign_up),
                    typography = typography.bodyMedium,
                    color = colorScheme.primary,
                    modifier = Modifier.pressScaleClickable(onClick = onRegisterClick)
                )
            }
        })
}