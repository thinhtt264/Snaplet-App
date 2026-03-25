package com.thinh.snaplet.di

import com.thinh.snaplet.platform.widget.WidgetUpdateManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetUpdateEntryPoint {
    fun widgetUpdateManager(): WidgetUpdateManager
}
