package com.thinh.snaplet.data.datasource.remote

import com.google.gson.Gson
import com.thinh.snaplet.data.model.ResponseStatus
import com.thinh.snaplet.data.model.StandardResponse
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.json.JSONObject
import java.io.IOException

class ResponseNormalizeInterceptor : Interceptor {
    private val gson = Gson()

    override fun intercept(chain: Interceptor.Chain): Response {
        return try {
            val response = chain.proceed(chain.request())

            if (response.isSuccessful) return response

            val body = response.body ?: return response
            val rawJson = body.string()

            val normalizedJson = normalizeError(
                rawJson = rawJson,
                httpCode = response.code
            )

            response.newBuilder()
                .body(normalizedJson.toResponseBody(body.contentType()))
                .build()
        } catch (_: IOException) {
            val request = chain.request()
            return Response.Builder()
                .request(request)
                .protocol(okhttp3.Protocol.HTTP_1_1)
                .code(500)
                .body("".toResponseBody())
                .message(createErrorMessage(500))
                .build()
        }
    }

    private fun createErrorMessage(httpCode: Int, rawJson: String? = null): String {
        return mapHttpMessage(httpCode) ?: extractMessage(rawJson) ?: "Lỗi không xác định"
    }

    private fun normalizeError(rawJson: String, httpCode: Int): String {
        val message = createErrorMessage(httpCode, rawJson)

        val standardError = StandardResponse(
            status = ResponseStatus(
                code = httpCode,
                message = message
            ),
            data = null
        )

        return gson.toJson(standardError)
    }

    private fun extractMessage(rawJson: String?): String? {
        return try {
            rawJson?.let {
                val obj = JSONObject(it)
                obj.optJSONObject("status")?.optString("message")
            }
        } catch (_: Exception) {
            null
        }
    }

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

