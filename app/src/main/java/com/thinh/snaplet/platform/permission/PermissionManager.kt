package com.thinh.snaplet.platform.permission

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

interface PermissionManager {

    fun hasPermission(permission: Permission): Boolean

    fun hasPermissions(vararg permissions: Permission): Boolean

    fun getPermissionState(permission: Permission): PermissionState
}

@Singleton
class PermissionManagerImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : PermissionManager {

    override fun hasPermission(permission: Permission): Boolean {
        if (permission is Permission.Notifications &&
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
        ) {
            return true
        }
        return ContextCompat.checkSelfPermission(
            context,
            permission.manifestPermission
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun hasPermissions(vararg permissions: Permission): Boolean {
        return permissions.all { hasPermission(it) }
    }

    override fun getPermissionState(permission: Permission): PermissionState {
        return if (hasPermission(permission)) {
            PermissionState.Granted
        } else {
            PermissionState.Denied
        }
    }
}

sealed class Permission(val manifestPermission: String) {
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    object Notifications : Permission(Manifest.permission.POST_NOTIFICATIONS)

    object Camera : Permission(Manifest.permission.CAMERA)
    object ReadExternalStorage : Permission(Manifest.permission.READ_EXTERNAL_STORAGE)
    object WriteExternalStorage : Permission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    object RecordAudio : Permission(Manifest.permission.RECORD_AUDIO)
    object AccessFineLocation : Permission(Manifest.permission.ACCESS_FINE_LOCATION)
    object AccessCoarseLocation : Permission(Manifest.permission.ACCESS_COARSE_LOCATION)
}

