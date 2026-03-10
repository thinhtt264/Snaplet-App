package com.thinh.snaplet.utils


object ValidationConstants {
    /**
     * Regex pattern for username validation Allows only letters (a-z, A-Z),
     * numbers (0-9), and underscores (_)
     */
    val USERNAME_PATTERN = Regex("^[a-zA-Z0-9_]+$")

    const val USERNAME_MIN_LENGTH = 3

    const val USERNAME_MAX_LENGTH = 20

    const val PASSWORD_MIN_LENGTH = 8

    const val CAPTION_MAX_LENGTH = 36
}