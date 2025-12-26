import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.coroutineScope

fun Modifier.thenIf(
    condition: Boolean, modifier: Modifier.() -> Modifier
): Modifier {
    return if (condition) this.then(modifier(this)) else this
}

fun Modifier.showIf(condition: Boolean): Modifier {
    return if (condition) this else Modifier.size(0.dp)
}

fun Modifier.pressScaleClickable(
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
    scaleOnPress: Float = 0.96f,
    onClick: suspend () -> Unit
): Modifier = composed {
    val internalInteractionSource = remember { MutableInteractionSource() }
    val finalInteractionSource = interactionSource ?: internalInteractionSource

    var isProcessing by remember { mutableStateOf(false) }
    val isPressed by finalInteractionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) scaleOnPress else 1f,
        animationSpec = if (isPressed) spring(
            stiffness = Spring.StiffnessHigh, dampingRatio = Spring.DampingRatioNoBouncy
        )
        else spring(
            stiffness = Spring.StiffnessMedium, dampingRatio = Spring.DampingRatioMediumBouncy
        ),
        label = "pressScaleDeferred"
    )

    this
        .scale(scale)
        .pointerInput(enabled) {
            if (!enabled) return@pointerInput

            detectTapGestures(
                onPress = { offset ->
                    if (isProcessing) return@detectTapGestures

                    isProcessing = true
                    val press = PressInteraction.Press(offset)
                    finalInteractionSource.tryEmit(press)

                    coroutineScope {
                        val released = tryAwaitRelease()

                        if (!released) {
                            finalInteractionSource.tryEmit(PressInteraction.Release(press))
                            isProcessing = false
                            return@coroutineScope
                        }

                        try {
                            onClick()
                        } finally {
                            finalInteractionSource.tryEmit(
                                PressInteraction.Release(press)
                            )
                            isProcessing = false
                        }
                    }
                })
        }
}