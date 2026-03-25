package com.thinh.snaplet

import android.annotation.SuppressLint
import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.WorkManager
import com.thinh.snaplet.utils.Logger
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class HiltApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()
        WorkManager.initialize(this, workManagerConfiguration)
        initializeTimber()
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    private fun initializeTimber() {
        if (BuildConfig.DEBUG) {
            Logger.plant(Timber.DebugTree())
        } else {
            Logger.plant(ReleaseTree())
        }
    }

    private class ReleaseTree : Timber.Tree() {
        @SuppressLint("LogNotTimber")
        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            if (priority == Log.ERROR || priority == Log.ASSERT) {
                Log.e("Timber", message, t)
                // Log to crash reporting service (Firebase Crashlytics, Sentry, etc.)
                // Example: FirebaseCrashlytics.getInstance().log("$tag: $message")
                // if (t != null) FirebaseCrashlytics.getInstance().recordException(t)
            }
        }
    }
}

