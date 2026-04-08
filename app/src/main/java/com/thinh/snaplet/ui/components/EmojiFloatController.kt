package com.thinh.snaplet.ui.components

import androidx.compose.ui.geometry.Size
import com.thinh.snaplet.domain.model.EmojiParticle
import com.thinh.snaplet.domain.model.FloatDirection
import com.thinh.snaplet.utils.EmojiParticleEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class EmojiFloatController {

    private companion object {
        const val MAX_ACTIVE_BATCHES = 3
    }

    /**
     * One optional “tracked” emit at a time (e.g. owner-viewed celebration with [onEnd]).
     * Each [emit] with [onEnd] is a single [EmojiParticleEngine.spawnBatch] → one [batchId]
     * for all particles in that animation; [MAX_ACTIVE_BATCHES] only caps concurrent emits overall.
     */
    private data class TrackedEnd(val batchId: Long, val onEnd: () -> Unit)

    private val _particles = MutableStateFlow<List<EmojiParticle>>(emptyList())
    val particles: StateFlow<List<EmojiParticle>> = _particles.asStateFlow()
    private var trackedEnd: TrackedEnd? = null

    private var canvasSize: Size = Size.Zero
    private var horizontalPaddingPx: Float = 0f

    fun onCanvasMeasured(width: Float, height: Float, paddingPx: Float) {
        canvasSize = Size(width, height)
        horizontalPaddingPx = paddingPx
    }

    fun emit(
        emoji: String,
        direction: FloatDirection = FloatDirection.UP,
        onEnd: (() -> Unit)? = null,
    ): Long? {
        if (canvasSize == Size.Zero) return null
        if (onEnd != null) {
            cancelTrackedAnimation()
        }
        val activeBatches = _particles.value.distinctBy { it.batchId }.size
        if (activeBatches >= MAX_ACTIVE_BATCHES) return null
        val batch = EmojiParticleEngine.spawnBatch(
            emoji = emoji,
            canvasWidth = canvasSize.width,
            canvasHeight = canvasSize.height,
            direction = direction,
            horizontalPaddingPx = horizontalPaddingPx,
        )
        val batchId = batch.firstOrNull()?.batchId ?: return null
        if (onEnd != null) {
            trackedEnd = TrackedEnd(batchId = batchId, onEnd = onEnd)
        }
        _particles.update { it + batch }
        return batchId
    }

    /** Stops the current tracked animation without invoking [onEnd]. Untracked emits (no callback) stay on screen. */
    fun cancelTrackedAnimation() {
        val batchId = trackedEnd?.batchId ?: return
        trackedEnd = null
        _particles.update { particles -> particles.filterNot { it.batchId == batchId } }
    }

    internal fun tick() {
        val current = _particles.value
        if (current.isEmpty()) return
        val previousBatchIds = current.asSequence().map { it.batchId }.toSet()
        val next = EmojiParticleEngine.nextFrame(
            particles = current,
            canvasHeight = canvasSize.height,
        )
        _particles.value = next
        val nextBatchIds = next.asSequence().map { it.batchId }.toSet()
        val finishedBatchIds = previousBatchIds - nextBatchIds
        val tracked = trackedEnd
        if (tracked != null && tracked.batchId in finishedBatchIds) {
            trackedEnd = null
            tracked.onEnd()
        }
    }
}
