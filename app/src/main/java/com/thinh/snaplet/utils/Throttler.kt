package com.thinh.snaplet.utils

class Throttler(private val timeout: Long) {
    private var lastTime = 0L

    fun run(block: () -> Unit) {
        val now = System.currentTimeMillis()
        if (now - lastTime >= timeout) {
            lastTime = now
            block()
        }
    }

    fun reset() {
        lastTime = 0L
    }
}
