package com.thinh.snaplet.data.model.post

import androidx.annotation.Keep

@Keep
data class NewPostUpdate(
    val count: Int,
    val seq: Int
)
