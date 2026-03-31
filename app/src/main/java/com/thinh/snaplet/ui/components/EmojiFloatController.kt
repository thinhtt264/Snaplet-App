package com.thinh.snaplet.ui.components

import androidx.compose.ui.geometry.Size
import com.thinh.snaplet.domain.model.EmojiParticle
import com.thinh.snaplet.domain.model.FloatDirection
import com.thinh.snaplet.utils.EmojiParticleEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class EmojiFloatController(
    private val maxActiveBatches: Int = MAX_ACTIVE_BATCHES,
) {

    private companion object {
        const val MAX_ACTIVE_BATCHES = 3
    }

    private val _particles = MutableStateFlow<List<EmojiParticle>>(emptyList())
    val particles: StateFlow<List<EmojiParticle>> = _particles.asStateFlow()

    private var direction: FloatDirection = FloatDirection.UP
    private var canvasSize: Size = Size.Zero
    private var horizontalPaddingPx: Float = 0f

    fun setDirection(direction: FloatDirection) {
        this.direction = direction
    }

    fun onCanvasMeasured(width: Float, height: Float, paddingPx: Float) {
        canvasSize = Size(width, height)
        horizontalPaddingPx = paddingPx
    }

    fun emit(emoji: String) {
        if (canvasSize == Size.Zero) return
        val activeBatches = _particles.value.distinctBy { it.batchId }.size
        if (activeBatches >= maxActiveBatches) return
        val batch = EmojiParticleEngine.spawnBatch(
            emoji = emoji,
            canvasWidth = canvasSize.width,
            canvasHeight = canvasSize.height,
            direction = direction,
            horizontalPaddingPx = horizontalPaddingPx,
        )
        _particles.update { it + batch }
    }

    internal fun tick() {
        val current = _particles.value
        if (current.isEmpty()) return
        _particles.value = EmojiParticleEngine.nextFrame(
            particles = current,
            canvasHeight = canvasSize.height,
        )
    }
}
