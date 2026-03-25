package com.thinh.snaplet.platform.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.thinh.snaplet.ui.widget.SnapletWidgetReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WidgetUpdateManager @Inject constructor(
    private val workManager: WorkManager,
    @ApplicationContext private val context: Context,
) {
    private fun hasAnySnapletWidget(): Boolean {
        val manager = AppWidgetManager.getInstance(context)
        val provider = ComponentName(context, SnapletWidgetReceiver::class.java)
        return manager.getAppWidgetIds(provider).isNotEmpty()
    }

    fun cancelWidgetBackgroundWork() {
        workManager.cancelUniqueWork(PERIODIC_WIDGET_UPDATE_WORK)
        workManager.cancelUniqueWork(UNIQUE_WIDGET_SYNC_NAME)
    }

    fun schedulePeriodicUpdate() {
        val request =
            PeriodicWorkRequestBuilder<WidgetUpdateWorker>(15, TimeUnit.MINUTES).setConstraints(
                widgetUpdateNetworkConstraints()
            ).setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                10,
                TimeUnit.SECONDS,
            ).build()

        workManager.enqueueUniquePeriodicWork(
            PERIODIC_WIDGET_UPDATE_WORK,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    fun scheduleImmediateUpdate(requireGlanceIds: Boolean = true) {
        if (requireGlanceIds && !hasAnySnapletWidget()) return
        val request = OneTimeWorkRequestBuilder<WidgetUpdateWorker>().setConstraints(
            widgetUpdateNetworkConstraints()
        ).build()
        workManager.enqueueUniqueWork(
            UNIQUE_WIDGET_SYNC_NAME,
            ExistingWorkPolicy.KEEP,
            request,
        )
    }

    companion object {
        const val UNIQUE_WIDGET_SYNC_NAME = "widget_sync"
        private const val PERIODIC_WIDGET_UPDATE_WORK = "periodic_widget_update_work"
    }
}
