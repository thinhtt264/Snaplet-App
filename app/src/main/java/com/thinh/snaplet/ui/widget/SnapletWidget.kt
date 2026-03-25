package com.thinh.snaplet.ui.widget

import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.currentState

class SnapletWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(
        context: android.content.Context,
        id: GlanceId,
    ) {
        provideContent {
            val prefs = currentState<Preferences>()
            val data = WidgetDisplayData.fromPreferences(prefs)
            SnapletWidgetContent(
                data = data,
                modifier = GlanceModifier,
            )
        }
    }
}
