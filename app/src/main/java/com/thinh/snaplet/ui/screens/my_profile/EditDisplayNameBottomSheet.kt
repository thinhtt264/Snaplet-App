package com.thinh.snaplet.ui.screens.my_profile

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.thinh.snaplet.R
import com.thinh.snaplet.ui.components.BaseText
import com.thinh.snaplet.ui.components.FormTextField
import com.thinh.snaplet.ui.components.PrimaryButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditDisplayNameBottomSheet(
    firstName: String,
    lastName: String,
    isSaving: Boolean,
    onFirstNameChange: (String) -> Unit,
    onLastNameChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSaveClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val focusManager = LocalFocusManager.current
    val firstNameFocusRequester = remember { FocusRequester() }
    val lastNameFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        firstNameFocusRequester.requestFocus()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.statusBarsPadding(),
    ) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(all = 20.dp)
                .pointerInput(Unit) {
                    detectTapGestures(onTap = {
                        focusManager.clearFocus()
                    })
                },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BaseText(
                text = stringResource(R.string.profile_edit_name_title),
                typography = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            FormTextField(
                value = firstName,
                onValueChange = onFirstNameChange,
                label = stringResource(R.string.first_name),
                placeholder = stringResource(R.string.first_name_placeholder),
                modifier = Modifier.focusRequester(firstNameFocusRequester),
                imeAction = ImeAction.Next,
                onImeAction = {
                    lastNameFocusRequester.requestFocus()
                },
            )

            Spacer(modifier = Modifier.height(16.dp))

            FormTextField(
                value = lastName,
                onValueChange = onLastNameChange,
                label = stringResource(R.string.last_name),
                placeholder = stringResource(R.string.last_name_placeholder),
                modifier = Modifier.focusRequester(lastNameFocusRequester),
                imeAction = ImeAction.Done,
                onImeAction = {
                    focusManager.clearFocus()
                },
            )

            Spacer(modifier = Modifier.height(32.dp))

            PrimaryButton(
                onClick = {
                    focusManager.clearFocus()
                    onSaveClick()
                },
                title = stringResource(R.string.profile_edit_name_save),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !isSaving,
                isLoading = isSaving,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                titleColor = Color.Black,
                typography = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

