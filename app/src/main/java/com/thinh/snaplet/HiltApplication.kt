package com.thinh.snaplet

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.WorkManager
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.thinh.snaplet.utils.CrashlyticsLogger
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
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)
        FirebaseCrashlytics.getInstance().setCustomKey("is_development", BuildConfig.IS_DEVELOPMENT)
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
        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            if (priority == Log.ERROR || priority == Log.ASSERT) {
//                Log.e("Timber", message, t)
                CrashlyticsLogger.error(tag, message, t)
            }
        }
    }
}