package com.thinh.snaplet.domain.model

data class EmojiParticle(
    val id: Long,
    val batchId: Long,
    val emoji: String,
    val x: Float,
    val y: Float,
    val size: Float,
    val speed: Float,
    val alpha: Float,
    val direction: FloatDirection,
    val frameCount: Int = 0,
)

enum class FloatDirection { UP, DOWN }
