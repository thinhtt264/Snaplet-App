package com.thinh.snaplet.data.model

/**
 * User profile data model
 * Represents user information displayed in overlay
 */
data class UserProfile(
    val userName: String,
    val displayName: String? = null,
    val avatarUrl: String? = null
)

