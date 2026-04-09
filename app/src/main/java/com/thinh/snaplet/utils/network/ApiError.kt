package com.thinh.snaplet.utils.network

data class ApiError(
    val httpCode: Int,
    val errorCode: ApiErrorCode? = null,
    val message: String,
    val reason: String? = null,
    val hoursRemaining: Int? = null,
)