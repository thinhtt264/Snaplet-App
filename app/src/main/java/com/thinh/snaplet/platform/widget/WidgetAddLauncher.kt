package com.thinh.snaplet.platform.widget

import android.app.Activity
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.thinh.snaplet.R
import com.thinh.snaplet.ui.widget.SnapletWidgetReceiver
import com.thinh.snaplet.utils.Logger

fun Context.launchSnapletWidgetPicker(onComplete: (() -> Unit)? = null) {
    val appWidgetManager = AppWidgetManager.getInstance(this)
    val provider = ComponentName(this, SnapletWidgetReceiver::class.java)

    if (appWidgetManager.isRequestPinAppWidgetSupported) {
        val callbackIntent = Intent(this, WidgetPinnedReceiver::class.java)
        val callbackPendingIntent = PendingIntent.getBroadcast(
            this,
            0,
            callbackIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val accepted = appWidgetManager.requestPinAppWidget(
            provider,
            null,
            callbackPendingIntent,
        )
        if (!accepted) {
            Toast.makeText(this, getString(R.string.profile_how_to_add_widget), Toast.LENGTH_SHORT)
                .show()
        }
        onComplete?.invoke()
        return
    }

    val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_PICK).apply {
        putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, provider)
        if (this@launchSnapletWidgetPicker !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching {
        appWidgetManager.getAppWidgetIds(provider)
        startActivity(intent)
    }.onFailure {
        Logger.e("❌ Cannot open widget picker: ${it.message}")
    }.also {
        onComplete?.invoke()
    }
}