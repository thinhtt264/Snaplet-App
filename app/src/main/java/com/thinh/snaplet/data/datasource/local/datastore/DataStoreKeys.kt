package com.thinh.snaplet.data.datasource.local.datastore

object DataStoreKeys {

    object SessionKeys {
        const val ACCESS_TOKEN = "session_access_token"
        const val REFRESH_TOKEN = "session_refresh_token"

        const val IS_COMPLETE_ONBOARDING = "session_complete_onboarding"
    }

    object UserProfileKeys {
        const val PROFILE = "user_profile"
    }

    object DeviceKeys {
        const val FINGERPRINT = "device_fingerprint"
    }

    object QuickChatKeys {
        const val RECENT_EMOJIS = "quick_chat_recent_emojis"
    }

    object ChatKeys {
        const val LOCAL_OWNER_USER_ID = "chat_local_owner_user_id"
    }
}

