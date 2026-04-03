package com.thinh.snaplet.utils

import com.google.firebase.crashlytics.FirebaseCrashlytics

object CrashlyticsLogger {
    fun screen(name: String) {
        FirebaseCrashlytics.getInstance().log("SCREEN: $name")
    }

    fun action(event: String, detail: String? = null) {
        val msg = if (detail != null) "ACTION: $event — $detail" else "ACTION: $event"
        FirebaseCrashlytics.getInstance().log(msg)
    }

    fun error(tag: String?, message: String, throwable: Throwable? = null) {
        FirebaseCrashlytics.getInstance().log("ERROR: [$tag] $message")
        throwable?.let { FirebaseCrashlytics.getInstance().recordException(it) }
    }

    fun setUser(userId: String) {
        FirebaseCrashlytics.getInstance().setUserId(userId)
    }
}