package com.thinh.snaplet.utils

import com.thinh.snaplet.domain.model.EmojiParticle
import com.thinh.snaplet.domain.model.FloatDirection
import kotlin.math.pow
import kotlin.random.Random

object EmojiParticleEngine {

    private const val ACCELERATION = 1.03f
    private const val PARTICLES_PER_BATCH = 16
    private const val BATCH_VARIANCE = 2
    private const val SIZE_MIN = 84f
    private const val SIZE_MAX = 160f
    private const val FADE_OUT_HEIGHT_FRAC_MIN = 0.7f
    private const val FADE_OUT_HEIGHT_FRAC_MAX = 0.97f
    private const val FADE_ZONE_HEIGHT_FRAC_MIN = 0.05f
    private const val FADE_ZONE_HEIGHT_FRAC_MAX = 0.08f
    private const val BASE_SPEED_MIN = 4f
    private const val BASE_SPEED_MAX = 10f
    private const val LARGE_GLYPH_SIZE_THRESHOLD_PX = SIZE_MAX + 1f

    private fun fadeOutYForFraction(
        canvasHeight: Float,
        direction: FloatDirection,
        fadeOutHeightFraction: Float,
        isLarge: Boolean,
        glyphSize: Float,
    ): Float = when (direction) {
        FloatDirection.UP ->
            if (isLarge) -glyphSize
            else canvasHeight * (1f - fadeOutHeightFraction)

        FloatDirection.DOWN ->
            if (isLarge) canvasHeight + glyphSize
            else canvasHeight * fadeOutHeightFraction
    }

    private const val EMIT_STAGGER_FRAMES_MAX = 45
    private const val EMIT_STAGGER_JITTER_FRAMES = 2

    private var nextId = 0L
    private var nextBatchId = 0L

    private fun staggeredEmitDelays(count: Int): List<Int> {
        if (count <= 0) return emptyList()
        val span = (count - 1).coerceAtLeast(1)
        val step = EMIT_STAGGER_FRAMES_MAX.toFloat() / span
        return List(count) { i ->
            val base = (i * step).toInt()
            val jitter = Random.nextInt(-EMIT_STAGGER_JITTER_FRAMES, EMIT_STAGGER_JITTER_FRAMES + 1)
            (base + jitter).coerceIn(0, EMIT_STAGGER_FRAMES_MAX)
        }.shuffled()
    }

    /** Evenly spaced 0…1 anchors (fixed order: balanced across drawable width per particle size). */
    private fun spreadHorizontalAnchors(count: Int): List<Float> {
        if (count <= 0) return emptyList()
        if (count == 1) return listOf(0.5f)
        val span = (count - 1).coerceAtLeast(1)
        return List(count) { i -> i.toFloat() / span.toFloat() }
    }

    fun spawnBatch(
        emoji: String,
        canvasWidth: Float,
        canvasHeight: Float,
        direction: FloatDirection,
        horizontalPaddingPx: Float = 0f,
    ): List<EmojiParticle> {
        val count = PARTICLES_PER_BATCH + Random.nextInt(-BATCH_VARIANCE, BATCH_VARIANCE + 1)
        val batchId = nextBatchId++
        val emitDelays = staggeredEmitDelays(count)
        val horizontalAnchors = spreadHorizontalAnchors(count)

        return List(count) {
            val size = Random.nextFloat() * (SIZE_MAX - SIZE_MIN) + SIZE_MIN
            val minX = horizontalPaddingPx
            val maxX = (canvasWidth - horizontalPaddingPx - size).coerceAtLeast(minX)
            val spanX = maxX - minX
            val x = minX + horizontalAnchors[it] * spanX
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
                emitDelayFramesRemaining = emitDelays[it],
                fadeOutHeightFraction = Random.nextFloat() *
                        (FADE_OUT_HEIGHT_FRAC_MAX - FADE_OUT_HEIGHT_FRAC_MIN) + FADE_OUT_HEIGHT_FRAC_MIN,
                fadeZoneHeightFraction = Random.nextFloat() *
                        (FADE_ZONE_HEIGHT_FRAC_MAX - FADE_ZONE_HEIGHT_FRAC_MIN) + FADE_ZONE_HEIGHT_FRAC_MIN,
            )
        }
    }

    fun nextFrame(
        particles: List<EmojiParticle>,
        canvasHeight: Float,
    ): List<EmojiParticle> {
        return particles.mapNotNull { p ->
            if (p.emitDelayFramesRemaining > 0) {
                return@mapNotNull p.copy(
                    emitDelayFramesRemaining = p.emitDelayFramesRemaining - 1,
                    alpha = 0f,
                )
            }

            val frame = p.frameCount + 1
            val currentSpeed = p.speed * ACCELERATION.pow(frame)

            val newY = when (p.direction) {
                FloatDirection.UP -> p.y - currentSpeed
                FloatDirection.DOWN -> p.y + currentSpeed
            }

            val isLarge = p.size >= LARGE_GLYPH_SIZE_THRESHOLD_PX
            val fadeOutY = fadeOutYForFraction(
                canvasHeight = canvasHeight,
                direction = p.direction,
                fadeOutHeightFraction = p.fadeOutHeightFraction,
                isLarge = isLarge,
                glyphSize = p.size,
            )
            val fadeZonePx = canvasHeight * p.fadeZoneHeightFraction

            val alpha = computeAlpha(
                newY,
                canvasHeight,
                p.direction,
                p.size,
                fadeOutY,
                fadeZonePx,
            )
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
        fadeZonePx: Float,
    ): Float {
        val fadeInAlpha = when (direction) {
            FloatDirection.UP -> {
                val spawnEdge = canvasHeight + size
                val fadeInEnd = spawnEdge - fadeZonePx
                if (y >= fadeInEnd) ((spawnEdge - y) / fadeZonePx).coerceIn(0f, 1f)
                else 1f
            }

            FloatDirection.DOWN -> {
                val spawnEdge = -size
                val fadeInEnd = spawnEdge + fadeZonePx
                if (y <= fadeInEnd) ((y - spawnEdge) / fadeZonePx).coerceIn(0f, 1f)
                else 1f
            }
        }

        val isLarge = size >= LARGE_GLYPH_SIZE_THRESHOLD_PX
        val fadeOutAlpha = when (direction) {
            FloatDirection.UP -> {
                if (isLarge) {
                    if (y <= fadeOutY) 0f else 1f
                } else {
                    val fadeStart = fadeOutY + fadeZonePx
                    if (y <= fadeStart) ((y - fadeOutY) / fadeZonePx).coerceIn(0f, 1f)
                    else 1f
                }
            }

            FloatDirection.DOWN -> {
                if (isLarge) {
                    if (y >= fadeOutY) 0f else 1f
                } else {
                    val fadeStart = fadeOutY - fadeZonePx
                    if (y >= fadeStart) ((fadeOutY - y) / fadeZonePx).coerceIn(0f, 1f)
                    else 1f
                }
            }
        }

        return (fadeInAlpha * fadeOutAlpha).coerceIn(0f, 1f)
    }
}
