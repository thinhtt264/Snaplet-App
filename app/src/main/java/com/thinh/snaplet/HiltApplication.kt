package com.thinh.snaplet

import android.app.Application
import com.thinh.snaplet.utils.Logger
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class HiltApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        initializeTimber()
    }
    
    private fun initializeTimber() {
        if (BuildConfig.DEBUG) {
            Logger.plant(Timber.DebugTree())
        } else {
            Logger.plant(ReleaseTree())
        }
    }
    
    /**
     * Custom tree for release builds
     * - Only logs ERROR and WTF
     * - Can integrate with Crashlytics, Firebase, etc.
     */
    private class ReleaseTree : Timber.Tree() {
        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            if (priority == android.util.Log.ERROR || priority == android.util.Log.ASSERT) {
                // Log to crash reporting service (Firebase Crashlytics, Sentry, etc.)
                // Example: FirebaseCrashlytics.getInstance().log("$tag: $message")
                // if (t != null) FirebaseCrashlytics.getInstance().recordException(t)
            }
        }
    }
}

