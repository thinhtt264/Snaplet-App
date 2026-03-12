package com.thinh.snaplet.domain.model

data class PostUpdateEvent(
    val type: String,
    val seq: Long,
    val count: Int,
)