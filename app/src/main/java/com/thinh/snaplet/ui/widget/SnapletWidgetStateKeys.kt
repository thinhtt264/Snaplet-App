package com.thinh.snaplet.ui.widget

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

object SnapletWidgetStateKeys {
    val POST_ID = stringPreferencesKey("post_id")
    val POST_IMAGE_URL = stringPreferencesKey("post_image_url")
    val POST_CAPTION = stringPreferencesKey("post_caption")
    val SENDER_AVATAR_URL = stringPreferencesKey("sender_avatar_url")
    val UNREAD_COUNT = intPreferencesKey("unread_count")
    val LAST_UPDATED_AT = longPreferencesKey("last_updated_at")
    val IS_ERROR = booleanPreferencesKey("is_error")
}
