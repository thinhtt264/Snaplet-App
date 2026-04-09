package com.thinh.snaplet.platform.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.updateAll
import androidx.work.BackoffPolicy
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.thinh.snaplet.ui.widget.SnapletWidget
import com.thinh.snaplet.ui.widget.SnapletWidgetReceiver
import com.thinh.snaplet.ui.widget.SnapletWidgetStateKeys
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
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    suspend fun clearAllWidgetState() {
        val glanceManager = GlanceAppWidgetManager(context)
        val glanceIds = glanceManager.getGlanceIds(SnapletWidget::class.java)
        if (glanceIds.isEmpty()) return

        glanceIds.forEach { glanceId ->
            updateAppWidgetState(
                context = context,
                glanceId = glanceId,
            ) { prefs ->
                clearWidgetPrefs(
                    prefs = prefs,
                )
            }
        }

        SnapletWidget().updateAll(context)
    }

    private fun clearWidgetPrefs(
        prefs: MutablePreferences,
    ) {
        prefs.remove(SnapletWidgetStateKeys.POST_IMAGE_URL)
        prefs.remove(SnapletWidgetStateKeys.POST_ID)
        prefs.remove(SnapletWidgetStateKeys.POST_CAPTION)
        prefs.remove(SnapletWidgetStateKeys.SENDER_AVATAR_URL)

        prefs[SnapletWidgetStateKeys.UNREAD_COUNT] = 0
        prefs.remove(SnapletWidgetStateKeys.LAST_UPDATED_AT)
        prefs[SnapletWidgetStateKeys.IS_ERROR] = false
    }

    companion object {
        const val UNIQUE_WIDGET_SYNC_NAME = "widget_sync"
        private const val PERIODIC_WIDGET_UPDATE_WORK = "periodic_widget_update_work"
    }
}
