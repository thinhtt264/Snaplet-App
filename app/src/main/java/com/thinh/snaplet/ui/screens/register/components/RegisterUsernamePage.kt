package com.thinh.snaplet.ui.screens.register.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.thinh.snaplet.R
import com.thinh.snaplet.ui.components.FormTextField
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RegisterUsernamePage(
    email: String,
    username: String,
    firstName: String,
    lastName: String,
    usernameError: String?,
    firstNameError: String?,
    lastNameError: String?,
    isLoading: Boolean,
    onUsernameChange: (String) -> Unit,
    onFirstNameChange: (String) -> Unit,
    onLastNameChange: (String) -> Unit,
    onContinue: () -> Unit,
    focusManager: FocusManager = LocalFocusManager.current
) {
    val usernameFocusRequester = remember { FocusRequester() }
    val firstNameFocusRequester = remember { FocusRequester() }
    val lastNameFocusRequester = remember { FocusRequester() }

    val firstNameBringIntoViewRequester = remember { BringIntoViewRequester() }
    val lastNameBringIntoViewRequester = remember { BringIntoViewRequester() }

    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        usernameFocusRequester.requestFocus()
    }

    RegisterPageContent(
        title = stringResource(R.string.register_username_title),
        subtitle = email,
        subtitleColor = colorScheme.primary,
        buttonText = stringResource(R.string.continue_text),
        buttonEnabled = !isLoading &&
            usernameError == null &&
            firstNameError == null &&
            lastNameError == null,
        isLoading = isLoading,
        onButtonClick = onContinue,
        inputField = {
            Column {
                FormTextField(
                    value = username,
                    onValueChange = onUsernameChange,
                    label = stringResource(R.string.username),
                    placeholder = stringResource(R.string.username),
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next,
                    onImeAction = {
                        firstNameFocusRequester.requestFocus()
                    },
                    errorMessage = usernameError,
                    enabled = !isLoading,
                    modifier = Modifier.focusRequester(usernameFocusRequester)
                )

                Spacer(Modifier.height(24.dp))

                FormTextField(
                    value = firstName,
                    onValueChange = onFirstNameChange,
                    label = stringResource(R.string.first_name),
                    placeholder = stringResource(R.string.first_name_placeholder),
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next,
                    onImeAction = {
                        lastNameFocusRequester.requestFocus()
                    },
                    errorMessage = firstNameError,
                    enabled = !isLoading,
                    modifier = Modifier
                        .focusRequester(firstNameFocusRequester)
                        .bringIntoViewRequester(firstNameBringIntoViewRequester)
                        .onFocusChanged { focusState ->
                            if (focusState.isFocused) {
                                coroutineScope.launch {
                                    firstNameBringIntoViewRequester.bringIntoView()
                                }
                            }
                        }
                )

                Spacer(Modifier.height(24.dp))

                FormTextField(
                    value = lastName,
                    onValueChange = onLastNameChange,
                    label = stringResource(R.string.last_name),
                    placeholder = stringResource(R.string.last_name_placeholder),
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Done,
                    onImeAction = {
                        focusManager.clearFocus()
                        onContinue()
                    },
                    errorMessage = lastNameError,
                    enabled = !isLoading,
                    modifier = Modifier
                        .focusRequester(lastNameFocusRequester)
                        .bringIntoViewRequester(lastNameBringIntoViewRequester)
                        .onFocusChanged { focusState ->
                            if (focusState.isFocused) {
                                coroutineScope.launch {
                                    lastNameBringIntoViewRequester.bringIntoView()
                                }
                            }
                        }
                )
            }
        }
    )
}

