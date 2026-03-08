package com.thinh.snaplet.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.navigation.NavBackStackEntry
import kotlinx.coroutines.flow.filterNotNull

interface NavResultKey<T : Any> {
    val key: String
}

object NavResultKeys {
    object CroppedUri : NavResultKey<String> {
        override val key = "cropped_uri"
    }
}

@Composable
fun <T : Any> CollectNavResult(
    key: NavResultKey<T>,
    onReceive: (T) -> Unit,
) {
    val backStackEntry = LocalViewModelStoreOwner.current as? NavBackStackEntry ?: return
    LaunchedEffect(Unit) {
        backStackEntry.savedStateHandle
            .getStateFlow<T?>(key.key, null)
            .filterNotNull()
            .collect { value ->
                backStackEntry.savedStateHandle.remove<T>(key.key)
                onReceive(value)
            }
    }
}