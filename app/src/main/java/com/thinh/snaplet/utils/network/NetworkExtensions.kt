package com.thinh.snaplet.utils.network

import com.thinh.snaplet.data.model.BaseResponse
import retrofit2.Response

private fun getDefaultMessage(code: Int): String? {
    return when (code) {
        400 -> "Yêu cầu không hợp lệ"
        401 -> "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại"
        408 -> "Yêu cầu hết thời gian chờ"
        429 -> "Bạn đã gửi quá nhiều yêu cầu. Vui lòng thử lại sau"
        500 -> "Lỗi máy chủ. Vui lòng thử lại sau"
        502 -> "Máy chủ tạm thời không khả dụng"
        503 -> "Dịch vụ đang bảo trì. Vui lòng thử lại sau"
        else -> null
    }
}

/** Convert error HTTP response to ApiError using BaseResponse structure */
fun Response<*>.toApiError(): ApiError {
    val rawJson = runCatching {
        errorBody()?.string()
    }.getOrNull()

    return runCatching {
        val base = GsonHolder.gson.fromJson(
            rawJson, BaseResponse::class.java
        ) as BaseResponse<*>

        val meta = base.status.meta
        val httpCode = code()

        val errorCode = meta?.errorCode?.let { raw ->
            runCatching { ApiErrorCode.valueOf(raw) }.getOrNull()
        }

        val message = getDefaultMessage(httpCode) ?: base.status.message ?: "Lỗi không xác định"

        ApiError(
            httpCode = httpCode,
            message = message,
            errorCode = errorCode,
            reason = meta?.reason,
            hoursRemaining = meta?.hoursRemaining,
        )
    }.getOrElse {
        ApiError(
            httpCode = 500, message = getDefaultMessage(500)!!
        )
    }
}

/** Safe API call wrapper with transform */
suspend fun <T, R> safeApiCall(
    apiCall: suspend () -> Response<BaseResponse<T>>,
    onSuccess: suspend (T) -> Unit = {},
    transform: (T) -> R
): ApiResult<R> {

    return try {
        val response = apiCall()

        if (response.isSuccessful) {
            val body = response.body() ?: return ApiResult.Failure(
                ApiError(
                    httpCode = 200, message = "Empty response body"
                )
            )
            onSuccess(body.data)
            ApiResult.Success(transform(body.data))
        } else {
            ApiResult.Failure(response.toApiError())
        }
    } catch (_: Exception) {
        ApiResult.Failure(
            ApiError(
                message = getDefaultMessage(500)!!, httpCode = 500
            )
        )
    }
}

suspend fun <T> safeApiCall(
    apiCall: suspend () -> Response<BaseResponse<T>>,
    onSuccess: suspend (T) -> Unit = {}
): ApiResult<T> {
    return safeApiCall(
        apiCall = apiCall,
        onSuccess = onSuccess,
        transform = { it }
    )
}

inline fun <T> ApiResult<T>.onSuccess(
    action: (T) -> Unit
): ApiResult<T> {
    if (this is ApiResult.Success) {
        action(data)
    }
    return this
}

inline fun <T> ApiResult<T>.onFailure(
    action: (ApiError) -> Unit
): ApiResult<T> {
    if (this is ApiResult.Failure) {
        action(error)
    }
    return this
}

inline fun <T> Result<T>.onSuccess(
    action: (T) -> Unit
): Result<T> {
    if (this.isSuccess) {
        action(getOrThrow())
    }
    return this
}

inline fun <T> Result<T>.onFailure(
    action: (Throwable) -> Unit
): Result<T> {
    if (this.isFailure) {
        action(exceptionOrNull()!!)
    }
    return this
}