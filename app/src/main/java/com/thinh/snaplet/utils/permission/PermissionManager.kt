package com.thinh.snaplet.utils.permission

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PermissionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    /**
     * Check if a permission is granted
     */
    fun hasPermission(permission: Permission): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            permission.manifestPermission
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Check multiple permissions at once
     */
    fun hasPermissions(vararg permissions: Permission): Boolean {
        return permissions.all { hasPermission(it) }
    }
    
    /**
     * Get permission state for UI decisions
     */
    fun getPermissionState(permission: Permission): PermissionState {
        return if (hasPermission(permission)) {
            PermissionState.Granted
        } else {
            PermissionState.Denied
        }
    }
}

/**
 * Sealed class defining all app permissions
 * Easy to extend for new permissions
 */
sealed class Permission(val manifestPermission: String) {
    object Camera : Permission(android.Manifest.permission.CAMERA)
    object ReadExternalStorage : Permission(android.Manifest.permission.READ_EXTERNAL_STORAGE)
    object WriteExternalStorage : Permission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
    object RecordAudio : Permission(android.Manifest.permission.RECORD_AUDIO)
    object AccessFineLocation : Permission(android.Manifest.permission.ACCESS_FINE_LOCATION)
    object AccessCoarseLocation : Permission(android.Manifest.permission.ACCESS_COARSE_LOCATION)
    
    // Easy to add more permissions here
    // object NewPermission : Permission(android.Manifest.permission.NEW_PERMISSION)
}

