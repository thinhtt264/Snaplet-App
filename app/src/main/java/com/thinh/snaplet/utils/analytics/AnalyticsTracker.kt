package com.thinh.snaplet.utils.analytics

import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent
import com.thinh.snaplet.BuildConfig
import javax.inject.Inject
import javax.inject.Singleton

object SignUpMethod {
    const val EMAIL = "email"
    const val GOOGLE = "google"
}

@Singleton
class AnalyticsTracker @Inject constructor(
    private val firebaseAnalytics: FirebaseAnalytics,
) {

    fun setUserId(userId: String?) {
        if (BuildConfig.IS_DEVELOPMENT) return
        firebaseAnalytics.setUserId(userId)
    }

    fun trackScreen(route: String) {
        if (BuildConfig.IS_DEVELOPMENT) return
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW) {
            param(FirebaseAnalytics.Param.SCREEN_NAME, route)
        }
    }

    fun trackSignUp(method: String) {
        if (BuildConfig.IS_DEVELOPMENT) return
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SIGN_UP) {
            param(FirebaseAnalytics.Param.METHOD, method)
        }
    }

    fun trackMessageSent(conversationId: String, messageType: String) {
        if (BuildConfig.IS_DEVELOPMENT) return
        firebaseAnalytics.logEvent("chat_message_sent") {
            param("conversation_id", conversationId)
            param("message_type", messageType)
        }
    }
}
