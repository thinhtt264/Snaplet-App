package com.thinh.snaplet.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
fun SearchBar(
    modifier: Modifier = Modifier,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    placeholder: String = "Enter your keyword"
) {
    val containerColor = MaterialTheme.colorScheme.surfaceContainer

    TextField(
        modifier = modifier,
        value = searchQuery,
        onValueChange = onSearchQueryChange,
        placeholder = { Text(text = placeholder) },
        colors = TextFieldDefaults.colors(
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            errorIndicatorColor = Color.Red,
            disabledContainerColor = containerColor,
            focusedContainerColor = containerColor,
            unfocusedContainerColor = containerColor,
        )
    )
}