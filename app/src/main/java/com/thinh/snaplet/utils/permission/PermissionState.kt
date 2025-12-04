package com.thinh.snaplet.utils.permission

/**
 * Sealed class representing permission states
 * Following Clean Architecture principles
 */
sealed class PermissionState {
    /** Permission is granted */
    object Granted : PermissionState()
    
    /** Permission is denied but can be requested */
    object Denied : PermissionState()
    
    /** Permission is permanently denied (user selected "Don't ask again") */
    object PermanentlyDenied : PermissionState()
    
    /** Permission request is in progress */
    object Requesting : PermissionState()
}

