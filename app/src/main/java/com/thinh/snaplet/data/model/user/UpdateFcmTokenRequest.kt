package com.thinh.snaplet.data.model.user

import com.google.gson.annotations.SerializedName

data class UpdateFcmTokenRequest(
    @SerializedName("fcmToken") val fcmToken: String,
)
