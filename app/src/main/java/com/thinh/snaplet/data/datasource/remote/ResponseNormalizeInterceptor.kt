package com.thinh.snaplet.data.datasource.remote

import com.thinh.snaplet.utils.Logger
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.json.JSONObject
import java.io.IOException

private const val DEFAULT_ERROR_MESSAGE = "Lỗi không xác định"

class ResponseNormalizeInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        return try {
            val response = chain.proceed(chain.request())

            if (response.isSuccessful) return response

            val body = response.body ?: return response

            val rawJson = body.string()

            val errorMessage = createErrorMessage(response.code, rawJson)

            response.newBuilder()
                .body(rawJson.toResponseBody(body.contentType()))
                .message(errorMessage)
                .build()
        } catch (_: IOException) {
            val request = chain.request()
            val errorMessage = createErrorMessage(500)
            return Response.Builder()
                .request(request)
                .protocol(okhttp3.Protocol.HTTP_1_1)
                .code(500)
                .body("".toResponseBody())
                .message(errorMessage)
                .build()
        }
    }

    private fun createErrorMessage(httpCode: Int, rawJson: String? = null): String {
        return mapHttpMessage(httpCode) ?: extractMessage(rawJson) ?: DEFAULT_ERROR_MESSAGE
    }

    private fun extractMessage(rawJson: String?): String? =
        runCatching {
            rawJson
                ?.let(::JSONObject)
                ?.optJSONObject("status")
                ?.optString("message")
                ?.takeIf { it.isNotBlank() }
        }.getOrNull()

    private fun mapHttpMessage(code: Int): String? = when (code) {
        400 -> "Yêu cầu không hợp lệ"
        401 -> "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại"
//        403 -> "Bạn không có quyền truy cập"
//        404 -> "Không tìm thấy dữ liệu"
        408 -> "Yêu cầu hết thời gian chờ"
        429 -> "Bạn đã gửi quá nhiều yêu cầu. Vui lòng thử lại sau"
        500 -> "Lỗi máy chủ. Vui lòng thử lại sau"
        502 -> "Máy chủ tạm thời không khả dụng"
        503 -> "Dịch vụ đang bảo trì. Vui lòng thử lại sau"
        else -> null
    }
}

