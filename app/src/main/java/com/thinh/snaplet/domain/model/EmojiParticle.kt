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
    /** Spawn position Y; fade-in uses this edge (includes random depth stagger, no frame delay). */
    val spawnY: Float,
    val fadeOutHeightFraction: Float,
    val fadeZoneHeightFraction: Float,
)

enum class FloatDirection { UP, DOWN }
