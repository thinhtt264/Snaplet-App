package com.thinh.snaplet.ui.widget

import androidx.datastore.preferences.core.Preferences

data class WidgetDisplayData(
    val postImageUrl: String?,
    val postCaption: String?,
    val senderAvatarUrl: String?,
    val unreadCount: Int,
    val lastUpdatedAt: Long?,
    val isLoading: Boolean,
    val isError: Boolean,
) {
    companion object {
        fun loading(): WidgetDisplayData {
            return WidgetDisplayData(
                postImageUrl = null,
                postCaption = null,
                senderAvatarUrl = null,
                unreadCount = 0,
                lastUpdatedAt = null,
                isLoading = true,
                isError = false,
            )
        }

        fun fromPreferences(prefs: Preferences): WidgetDisplayData {
            return WidgetDisplayData(
                postImageUrl = prefs[SnapletWidgetStateKeys.POST_IMAGE_URL],
                postCaption = prefs[SnapletWidgetStateKeys.POST_CAPTION],
                senderAvatarUrl = prefs[SnapletWidgetStateKeys.SENDER_AVATAR_URL],
                unreadCount = prefs[SnapletWidgetStateKeys.UNREAD_COUNT] ?: 0,
                lastUpdatedAt = prefs[SnapletWidgetStateKeys.LAST_UPDATED_AT],
                isLoading = prefs[SnapletWidgetStateKeys.IS_LOADING] ?: false,
                isError = prefs[SnapletWidgetStateKeys.IS_ERROR] ?: false,
            )
        }
    }
}
