package com.thinh.snaplet.data.model

import com.google.gson.annotations.SerializedName

data class ResponseStatus(
    @SerializedName("code")
    val code: Int,

    @SerializedName("message")
    val message: String? = null,

    @SerializedName("meta")
    val meta: ErrorMeta? = null
)

data class ErrorMeta(
    @SerializedName("errorCode")
    val errorCode: String? = null,

    @SerializedName("message")
    val message: String? = null,

    @SerializedName("reason")
    val reason: String? = null,

    @SerializedName("hoursRemaining")
    val hoursRemaining: Int? = null,
)

data class BaseResponse<T>(
    @SerializedName("status")
    val status: ResponseStatus,

    @SerializedName("data")
    val data: T
)

data class Pagination(
    @SerializedName("limit")
    val limit: Int,

    @SerializedName("nextCursor")
    val nextCursor: String? = null,
)

data class PaginatedResponse<T>(
    @SerializedName("data")
    val data: List<T>,

    @SerializedName("pagination")
    val pagination: Pagination,
)
