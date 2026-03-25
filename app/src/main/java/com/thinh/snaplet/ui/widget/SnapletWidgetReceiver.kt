package com.thinh.snaplet.ui.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import com.thinh.snaplet.di.WidgetUpdateEntryPoint
import com.thinh.snaplet.platform.widget.WidgetUpdateManager
import dagger.hilt.android.EntryPointAccessors

class SnapletWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SnapletWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
    }

    override fun onDisabled(context: Context) {
        widgetUpdateManager(context).cancelWidgetBackgroundWork()
        super.onDisabled(context)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        widgetUpdateManager(context).scheduleImmediateUpdate(requireGlanceIds = false)
    }

    private fun widgetUpdateManager(context: Context): WidgetUpdateManager =
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            WidgetUpdateEntryPoint::class.java,
        ).widgetUpdateManager()
}
