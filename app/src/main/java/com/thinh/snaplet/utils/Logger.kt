package com.thinh.snaplet.utils

import timber.log.Timber

/**
 * Logger - Centralized logging utility using Timber
 * 
 * Benefits:
 * - Auto-tagging with class name
 * - Logs only in debug builds (configurable)
 * - Consistent logging across the app
 * - Easy to disable in production
 * 
 * Usage:
 * ```
 * Logger.d("Debug message")
 * Logger.e(exception, "Error occurred")
 * Logger.i("Info message")
 * ```
 */
object Logger {
    
    /**
     * Log verbose message
     */
    fun v(message: String, vararg args: Any?) {
        Timber.v(message, *args)
    }
    
    /**
     * Log debug message
     */
    fun d(message: String, vararg args: Any?) {
        Timber.d(message, *args)
    }
    
    /**
     * Log info message
     */
    fun i(message: String, vararg args: Any?) {
        Timber.i(message, *args)
    }
    
    /**
     * Log warning message
     */
    fun w(message: String, vararg args: Any?) {
        Timber.w(message, *args)
    }
    
    /**
     * Log warning with throwable
     */
    fun w(throwable: Throwable, message: String = "", vararg args: Any?) {
        if (message.isEmpty()) {
            Timber.w(throwable)
        } else {
            Timber.w(throwable, message, *args)
        }
    }
    
    /**
     * Log error message
     */
    fun e(message: String, vararg args: Any?) {
        Timber.e(message, *args)
    }
    
    /**
     * Log error with throwable
     */
    fun e(throwable: Throwable, message: String = "", vararg args: Any?) {
        if (message.isEmpty()) {
            Timber.e(throwable)
        } else {
            Timber.e(throwable, message, *args)
        }
    }
    
    /**
     * Log WTF (What a Terrible Failure) - for critical errors
     */
    fun wtf(message: String, vararg args: Any?) {
        Timber.wtf(message, *args)
    }
    
    /**
     * Log WTF with throwable
     */
    fun wtf(throwable: Throwable, message: String = "", vararg args: Any?) {
        if (message.isEmpty()) {
            Timber.wtf(throwable)
        } else {
            Timber.wtf(throwable, message, *args)
        }
    }
    
    /**
     * Plant a custom tree (for custom logging behavior)
     */
    fun plant(tree: Timber.Tree) {
        Timber.plant(tree)
    }
    
    /**
     * Uproot a tree
     */
    fun uproot(tree: Timber.Tree) {
        Timber.uproot(tree)
    }
    
    /**
     * Uproot all trees
     */
    fun uprootAll() {
        Timber.uprootAll()
    }
}

