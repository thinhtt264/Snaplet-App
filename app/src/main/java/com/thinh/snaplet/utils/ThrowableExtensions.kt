package com.thinh.snaplet.utils

val Throwable.safeMessage: String
    get() = message?.takeIf { it.isNotBlank() } ?: "Lỗi không xác định"