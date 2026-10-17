package app.katachi.ui

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.katachi.R
import app.katachi.core.BuildRule
import app.katachi.core.DailySet
import app.katachi.core.GridMath
import app.katachi.core.Kind
import app.katachi.store
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** Time since the puzzle opened, frozen once it is solved. */
class Clock {
    private val start = System.currentTimeMillis()
    var stoppedAt: Long? = null
        private set

    fun stop(): Int {
        if (stoppedAt == null) stoppedAt = System.currentTimeMillis()
        return seconds(stoppedAt!!)
    }

    fun seconds(now: Long = System.currentTimeMillis()) = ((now - start) / 1000).toInt()
}

fun formatSeconds(seconds: Int) = "%d:%02d".format(seconds / 60, seconds % 60)

/** Small taps of the vibration motor: a tick for a snap, a buzz for a wrong answer, a thud for a solve. */
class Haptics(private val view: View) {
    fun tick() = view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)

    fun wrong() = view.performHapticFeedback(
        if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.REJECT else HapticFeedbackConstants.LONG_PRESS,
    )

    fun solved() = view.performHapticFeedback(
        if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.LONG_PRESS,
    )
}

@Composable
fun rememberHaptics(): Haptics {
    val view = LocalView.current
    return remember(view) { Haptics(view) }
}

/** Which of the day's puzzles are solved, in [DailySet.order]. */
@Composable
fun dayProgress(set: DailySet): List<Boolean> {
    val history by LocalContext.current.store.history.collectAsState()
    val day = history.day(set.date)
    return DailySet.order.map { (kind, i) -> day.solved(DailySet.id(kind, i)) }
}

fun stepOf(kind: Kind, index: Int) = DailySet.order.indexOf(kind to index)

/** Where a puzzle sits in the day: one segment per puzzle, solved ones filled, this one outlined. */
@Composable
fun DayProgress(solved: List<Boolean>, current: Int, modifier: Modifier = Modifier) {
    val done = MaterialTheme.colorScheme.primary
    val todo = MaterialTheme.colorScheme.surfaceVariant
    val here = MaterialTheme.colorScheme.onSurface
    Canvas(modifier.fillMaxWidth().height(6.dp)) {
        val gap = 4.dp.toPx()
        val w = (size.width - gap * (solved.size - 1)) / solved.size
        val r = CornerRadius(size.height / 2)
        solved.forEachIndexed { i, isDone ->
            val at = Offset(i * (w + gap), 0f)
            drawRoundRect(if (isDone) done else todo, at, Size(w, size.height), r)
            if (i == current && !isDone) drawRoundRect(here, at, Size(w, size.height), r, style = Stroke(1.5.dp.toPx()))
        }
    }
}

/** The frame every puzzle sits in: back, title, level, a running clock, and the day's progress. */
@Composable
fun PuzzleFrame(
    title: String,
    level: Int,
    clock: Clock,
    solved: Boolean,
    progress: List<Boolean>,
    step: Int,
    onBack: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(solved) {
        while (!solved) {
            now = System.currentTimeMillis()
            delay(1000)
        }
    }
    Column(Modifier.fillMaxSize().safeDrawingPadding()) {
        Row(Modifier.fillMaxWidth().padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.back))
            }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleLarge)
                Text(
                    stringResource(R.string.level, level),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.padding(end = 12.dp),
            ) {
                Text(
                    formatSeconds(clock.seconds(clock.stoppedAt ?: now)),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                )
            }
        }
        DayProgress(progress, step, Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
        content()
    }
}

/** Slides up under a solved puzzle: the time, what it was about, and the way on. */
@Composable
fun SolvedPanel(visible: Boolean, seconds: Int, note: String?, onNext: () -> Unit) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) { it } + fadeIn(),
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = MaterialTheme.shapes.extraLarge,
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(
                        stringResource(R.string.solved_in, formatSeconds(seconds)),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
                if (note != null) Text(note, style = MaterialTheme.typography.bodyMedium)
                Row {
                    Spacer(Modifier.weight(1f))
                    Button(onClick = onNext) { Text(stringResource(R.string.next)) }
                }
            }
        }
    }
}

private class Spark(val angle: Float, val speed: Float, val spin: Float, val color: Color, val size: Float)

private val sparkColors = listOf(0xFFE0614F, 0xFF3F8FD2, 0xFFF2B33D, 0xFF8E6BC8, 0xFF4DB38E, 0xFFE58AB4, 0xFF53B8C9)

/** A burst of falling confetti from the upper middle, played once when [play] turns true. */
@Composable
fun Confetti(play: Boolean, modifier: Modifier = Modifier) {
    if (!play) return
    val sparks = remember {
        List(70) {
            Spark(
                angle = Random.nextFloat() * 6.283f,
                speed = 0.4f + Random.nextFloat(),
                spin = Random.nextFloat() * 4f,
                color = Color(sparkColors.random()),
                size = 14f + Random.nextFloat() * 14f,
            )
        }
    }
    val t = remember { Animatable(0f) }
    LaunchedEffect(Unit) { t.animateTo(1f, tween(1600, easing = FastOutSlowInEasing)) }
    Canvas(modifier.fillMaxSize()) {
        if (t.value >= 1f) return@Canvas
        val centre = Offset(size.width / 2, size.height * 0.35f)
        val reach = size.minDimension * 0.6f
        for (s in sparks) {
            val d = reach * s.speed * t.value
            val fall = size.height * 0.4f * t.value * t.value
            val p = centre + Offset(cos(s.angle) * d, sin(s.angle) * d + fall)
            rotate(s.spin * t.value * 360f, p) {
                drawRect(s.color.copy(alpha = 1f - t.value), p - Offset(s.size / 2, s.size / 4), Size(s.size, s.size / 2))
            }
        }
    }
}

/**
 * A 6 × 6 pattern. [given] marks cells drawn on a tinted ground (fixed cells in pattern building),
 * [flagged] cells get a red wash, and [guide] draws the rule's mirror lines or tile edge.
 */
@Composable
fun GridView(
    grid: DoubleArray,
    modifier: Modifier = Modifier,
    ink: Color = MaterialTheme.colorScheme.onSurface,
    given: ((Int) -> Boolean)? = null,
    flagged: Set<Int> = emptySet(),
    guide: BuildRule? = null,
) {
    val line = MaterialTheme.colorScheme.outline
    val tint = MaterialTheme.colorScheme.surfaceVariant
    val error = MaterialTheme.colorScheme.error
    val accent = MaterialTheme.colorScheme.primary
    val inkGiven = lerp(ink, tint, 0.3f)
    Canvas(modifier.aspectRatio(1f).clip(RoundedCornerShape(6.dp))) {
        val n = GridMath.N
        val cell = size.width / n
        for (i in 0 until GridMath.CELLS) {
            val at = Offset((i % n) * cell, (i / n) * cell)
            val isGiven = given?.invoke(i) == true
            if (isGiven) drawRect(tint, at, Size(cell, cell))
            if (i in flagged) drawRect(error.copy(alpha = 0.3f), at, Size(cell, cell))
            if (grid[i] == 1.0) {
                val inset = cell * 0.08f
                drawRoundRect(
                    if (isGiven) inkGiven else ink, at + Offset(inset, inset), Size(cell - 2 * inset, cell - 2 * inset),
                    CornerRadius(cell * 0.14f),
                )
            }
        }
        val stroke = 1.dp.toPx()
        for (k in 0..n) {
            drawLine(line, Offset(k * cell, 0f), Offset(k * cell, size.height), stroke)
            drawLine(line, Offset(0f, k * cell), Offset(size.width, k * cell), stroke)
        }
        val dash = PathEffect.dashPathEffect(floatArrayOf(14f, 10f))
        val mid = size.width / 2
        val width = 2.5.dp.toPx()
        when (guide) {
            BuildRule.MIRROR -> drawLine(accent, Offset(mid, 0f), Offset(mid, size.height), width, pathEffect = dash)
            BuildRule.HALF_TURN -> drawCircle(accent, 5.dp.toPx(), Offset(mid, mid))
            BuildRule.QUARTER -> {
                drawLine(accent, Offset(mid, 0f), Offset(mid, size.height), width, pathEffect = dash)
                drawLine(accent, Offset(0f, mid), Offset(size.width, mid), width, pathEffect = dash)
            }
            BuildRule.TILE -> drawRect(accent, Offset.Zero, Size(mid, mid), style = Stroke(width, pathEffect = dash))
            null -> Unit
        }
    }
}

/** A springy pop whenever [trigger] changes: shrinks a touch, then bounces back to 1. */
@Composable
fun popScale(trigger: Any?): Float {
    val scale = remember { Animatable(1f) }
    LaunchedEffect(trigger) {
        if (trigger == null) return@LaunchedEffect
        scale.snapTo(0.9f)
        scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
    }
    return scale.value
}

@Composable
fun animatedFraction(target: Float): Float = animateFloatAsState(target, tween(400), label = "fraction").value
