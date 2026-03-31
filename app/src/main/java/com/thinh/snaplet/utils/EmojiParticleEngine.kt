package com.thinh.snaplet.utils

import com.thinh.snaplet.domain.model.EmojiParticle
import com.thinh.snaplet.domain.model.FloatDirection
import kotlin.math.pow
import kotlin.random.Random

object EmojiParticleEngine {

    private const val ACCELERATION = 1.042f
    private const val PARTICLES_PER_BATCH = 16
    private const val BATCH_VARIANCE = 2
    private const val SIZE_MIN = 52f
    private const val SIZE_MAX = 140f
    private const val FADE_ZONE_PX = 90f
    private const val BASE_SPEED_MIN = 10f
    private const val BASE_SPEED_MAX = 18f
    private const val LARGE_THRESHOLD = BASE_SPEED_MAX / 1.5f

    private var nextId = 0L
    private var nextBatchId = 0L

    fun spawnBatch(
        emoji: String,
        canvasWidth: Float,
        canvasHeight: Float,
        direction: FloatDirection,
        horizontalPaddingPx: Float = 0f,
    ): List<EmojiParticle> {
        val count = PARTICLES_PER_BATCH + Random.nextInt(-BATCH_VARIANCE, BATCH_VARIANCE + 1)
        val batchId = nextBatchId++

        return List(count) {
            val size = Random.nextFloat() * (SIZE_MAX - SIZE_MIN) + SIZE_MIN
            val minX = horizontalPaddingPx
            val maxX = (canvasWidth - horizontalPaddingPx - size).coerceAtLeast(minX)
            val x = minX + Random.nextFloat() * (maxX - minX)
            val startY = when (direction) {
                FloatDirection.UP -> canvasHeight + size
                FloatDirection.DOWN -> -size
            }
            EmojiParticle(
                id = nextId++,
                batchId = batchId,
                emoji = emoji,
                x = x,
                y = startY,
                size = size,
                speed = Random.nextFloat() * (BASE_SPEED_MAX - BASE_SPEED_MIN) + BASE_SPEED_MIN,
                alpha = 0f,
                direction = direction,
            )
        }
    }

    fun nextFrame(
        particles: List<EmojiParticle>,
        canvasHeight: Float,
    ): List<EmojiParticle> {
        return particles.mapNotNull { p ->
            val frame = p.frameCount + 1
            val currentSpeed = p.speed * ACCELERATION.pow(frame)

            val newY = when (p.direction) {
                FloatDirection.UP -> p.y - currentSpeed
                FloatDirection.DOWN -> p.y + currentSpeed
            }

            val isLarge = p.size >= LARGE_THRESHOLD
            val fadeOutY = when (p.direction) {
                FloatDirection.UP -> if (isLarge) -p.size else canvasHeight * 0.45f
                FloatDirection.DOWN -> if (isLarge) canvasHeight + p.size else canvasHeight * 0.55f
            }

            val alpha = computeAlpha(newY, canvasHeight, p.direction, p.size, fadeOutY)
            if (alpha <= 0f) return@mapNotNull null

            p.copy(
                y = newY,
                alpha = alpha,
                frameCount = frame,
            )
        }
    }

    private fun computeAlpha(
        y: Float,
        canvasHeight: Float,
        direction: FloatDirection,
        size: Float,
        fadeOutY: Float,
    ): Float {
        val fadeInAlpha = when (direction) {
            FloatDirection.UP -> {
                val spawnEdge = canvasHeight + size
                val fadeInEnd = spawnEdge - FADE_ZONE_PX
                if (y >= fadeInEnd) ((spawnEdge - y) / FADE_ZONE_PX).coerceIn(0f, 1f)
                else 1f
            }

            FloatDirection.DOWN -> {
                val spawnEdge = -size
                val fadeInEnd = spawnEdge + FADE_ZONE_PX
                if (y <= fadeInEnd) ((y - spawnEdge) / FADE_ZONE_PX).coerceIn(0f, 1f)
                else 1f
            }
        }

        val isLarge = size >= LARGE_THRESHOLD
        val fadeOutAlpha = when (direction) {
            FloatDirection.UP -> {
                if (isLarge) {
                    if (y <= fadeOutY) 0f else 1f
                } else {
                    val fadeStart = fadeOutY + FADE_ZONE_PX
                    if (y <= fadeStart) ((y - fadeOutY) / FADE_ZONE_PX).coerceIn(0f, 1f)
                    else 1f
                }
            }

            FloatDirection.DOWN -> {
                if (isLarge) {
                    if (y >= fadeOutY) 0f else 1f
                } else {
                    val fadeStart = fadeOutY - FADE_ZONE_PX
                    if (y >= fadeStart) ((fadeOutY - y) / FADE_ZONE_PX).coerceIn(0f, 1f)
                    else 1f
                }
            }
        }

        return (fadeInAlpha * fadeOutAlpha).coerceIn(0f, 1f)
    }
}
