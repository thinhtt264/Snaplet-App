package com.thinh.snaplet.platform.widget

import androidx.work.Constraints
import androidx.work.NetworkType

/**
 * Widget payload fetch needs network (real API). WorkManager defers work until satisfied,
 * so we do not flash loading/error while offline.
 */
fun widgetUpdateNetworkConstraints(): Constraints =
    Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()
