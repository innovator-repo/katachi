package app.katachi.designsystem

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/** The ecosystem's seven touch signatures. */
enum class HapticToken { TICK, CLICK, CHECK_COMPLETE, SEAL_LOCK, REVEAL_SWELL, BREATH_WAVE, GENTLE_NUDGE }

/** One step of a recipe: a primitive at a strength, [delayMs] after the one before it. */
data class Step(val primitive: Int, val scale: Float, val delayMs: Int)

/** The recipes as data, so they stay identical across the apps and are easy to check. */
object HapticRecipes {
    // VibrationEffect.Composition.PRIMITIVE_*, spelled out because they need API 30.
    const val CLICK = 1
    const val SLOW_RISE = 5
    const val QUICK_FALL = 6
    const val TICK = 7
    const val LOW_TICK = 8
    const val QUICK_RISE = 4

    fun of(token: HapticToken): List<Step> = when (token) {
        HapticToken.TICK -> listOf(Step(TICK, 0.50f, 0))
        HapticToken.CLICK -> listOf(Step(CLICK, 0.65f, 0))
        HapticToken.CHECK_COMPLETE -> listOf(Step(CLICK, 0.90f, 0), Step(TICK, 0.35f, 50))
        HapticToken.SEAL_LOCK -> listOf(Step(QUICK_RISE, 0.55f, 0), Step(CLICK, 1.0f, 30), Step(TICK, 0.40f, 70))
        HapticToken.REVEAL_SWELL -> listOf(Step(SLOW_RISE, 0.60f, 0), Step(TICK, 0.50f, 20))
        HapticToken.BREATH_WAVE -> listOf(Step(SLOW_RISE, 0.70f, 0), Step(TICK, 0.40f, 4000), Step(QUICK_FALL, 0.70f, 4000))
        HapticToken.GENTLE_NUDGE -> listOf(Step(LOW_TICK, 0.75f, 0), Step(LOW_TICK, 0.75f, 100))
    }

    /** Waveform for phones without the primitives: [pause, buzz, pause, buzz...] in ms, strongest steps longest. */
    fun fallback(token: HapticToken): Pair<LongArray, IntArray> {
        val timings = ArrayList<Long>()
        val amplitudes = ArrayList<Int>()
        for (step in of(token)) {
            val pause = minOf(step.delayMs, 120).toLong()
            timings += pause; amplitudes += 0
            timings += (8 + 22 * step.scale).toLong(); amplitudes += (255 * step.scale).toInt().coerceIn(1, 255)
        }
        return timings.toLongArray() to amplitudes.toIntArray()
    }
}

/** Plays [HapticToken]s on the vibration motor; silent when there is none. */
class LumeHaptics(context: Context) {
    private val vibrator: Vibrator? = runCatching {
        if (Build.VERSION.SDK_INT >= 31) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION") context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }.getOrNull()?.takeIf { it.hasVibrator() }

    fun play(token: HapticToken) {
        val v = vibrator ?: return
        runCatching { v.vibrate(effect(v, token)) }
    }

    private fun effect(v: Vibrator, token: HapticToken): VibrationEffect {
        val steps = HapticRecipes.of(token)
        if (Build.VERSION.SDK_INT >= 30 && v.areAllPrimitivesSupported(*steps.map { it.primitive }.toIntArray())) {
            val composition = VibrationEffect.startComposition()
            for (s in steps) composition.addPrimitive(s.primitive, s.scale, s.delayMs)
            return composition.compose()
        }
        val (timings, amplitudes) = HapticRecipes.fallback(token)
        return VibrationEffect.createWaveform(timings, amplitudes, -1)
    }
}

@androidx.compose.runtime.Composable
fun rememberLumeHaptics(): LumeHaptics {
    val context = androidx.compose.ui.platform.LocalContext.current.applicationContext
    return androidx.compose.runtime.remember(context) { LumeHaptics(context) }
}
