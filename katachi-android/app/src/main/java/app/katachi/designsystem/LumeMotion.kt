package app.katachi.designsystem

import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics

/** The ecosystem's motion tokens. */
object LumeMotion {
    const val PRESSED_SCALE = 0.97f

    /** Movement through space: overshoots a little. */
    fun <T> spatial(): FiniteAnimationSpec<T> = spring(dampingRatio = 0.75f, stiffness = 380f)

    /** Colour, opacity, progress: never overshoots. */
    fun <T> effects(): FiniteAnimationSpec<T> = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 1500f)

    /** One breath in or out. */
    fun <T> breathing(): FiniteAnimationSpec<T> = tween(4000, easing = EaseInOutSine)
}

/** Sinks to 0.97 under the finger with a TICK, springs back on release, and calls [onClick] on a tap. */
fun Modifier.pressable(enabled: Boolean = true, onClick: () -> Unit): Modifier = composed {
    val haptics = rememberLumeHaptics()
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        if (pressed) LumeMotion.PRESSED_SCALE else 1f, LumeMotion.spatial(), label = "press",
    )
    this
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .semantics { role = Role.Button }
        .pointerInput(enabled) {
            if (!enabled) return@pointerInput
            detectTapGestures(
                onPress = {
                    pressed = true
                    haptics.play(HapticToken.TICK)
                    tryAwaitRelease()
                    pressed = false
                },
                onTap = { onClick() },
            )
        }
}
