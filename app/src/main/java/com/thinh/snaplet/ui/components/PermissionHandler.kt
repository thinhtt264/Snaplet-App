package com.thinh.snaplet.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import com.thinh.snaplet.utils.Logger
import com.thinh.snaplet.utils.permission.Permission

/**
 * PermissionHandler - Composable để handle permission requests
 * 
 * Tách biệt logic permission khỏi UI components
 * Có thể reuse cho bất kỳ permission nào
 * 
 * Usage:
 * ```
 * PermissionHandler(
 *     permission = Permission.Camera,
 *     onPermissionResult = { granted ->
 *         if (granted) {
 *             // Use feature
 *         }
 *     }
 * ) { requestPermission ->
 *     Button(onClick = { requestPermission() }) {
 *         Text("Enable Camera")
 *     }
 * }
 * ```
 */
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
    
    val requestPermission = remember {
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
    
    val requestPermissions = remember {
        {
            Logger.d("Requesting permissions: ${permissions.map { it.manifestPermission }}")
            launcher.launch(permissions.map { it.manifestPermission }.toTypedArray())
        }
    }
    
    content(requestPermissions)
}

