package com.thinh.snaplet.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.thinh.snaplet.platform.permission.Permission
import com.thinh.snaplet.utils.Logger

@Composable
fun PermissionHandler(
    permission: Permission,
    onPermissionResult: (Boolean) -> Unit,
    content: @Composable (requestPermission: () -> Unit) -> Unit
) {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        Logger.d("Permission ${permission.manifestPermission}: $isGranted")
        onPermissionResult(isGranted)
    }

    val requestPermission = remember(permission) {
        {
            Logger.d("Requesting permission: ${permission.manifestPermission}")
            launcher.launch(permission.manifestPermission)
        }
    }

    content(requestPermission)
}

/**
 * Multiple permissions handler
 */
@Composable
fun MultiplePermissionsHandler(
    permissions: List<Permission>,
    onPermissionsResult: (Map<String, Boolean>) -> Unit,
    content: @Composable (requestPermissions: () -> Unit) -> Unit
) {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsMap ->
        Logger.d("Multiple permissions result: $permissionsMap")
        onPermissionsResult(permissionsMap)
    }

    val requestPermissions = remember(permissions) {
        {
            Logger.d("Requesting permissions: ${permissions.map { it.manifestPermission }}")
            launcher.launch(permissions.map { it.manifestPermission }.toTypedArray())
        }
    }

    content(requestPermissions)
}
