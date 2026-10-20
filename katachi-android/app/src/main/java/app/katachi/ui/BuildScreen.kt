package app.katachi.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import app.katachi.LumeCore
import app.katachi.R
import app.katachi.core.DailySet
import app.katachi.core.GridMath
import app.katachi.core.Kind
import app.katachi.core.Result
import app.katachi.store
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun BuildScreen(set: DailySet, index: Int, onBack: () -> Unit, onNext: () -> Unit) {
    val puzzle = set.builds[index]
    val store = LocalContext.current.store
    val haptics = rememberHaptics()
    val scope = rememberCoroutineScope()
    val clock = remember(puzzle) { Clock() }
    val cells = remember(puzzle) { mutableStateListOf<Double>().apply { addAll(puzzle.given.toList()) } }
    val flagged = remember(puzzle) { mutableStateListOf<Int>() }
    var tries by remember(puzzle) { mutableIntStateOf(0) }
    var off by remember(puzzle) { mutableStateOf<Int?>(null) }
    var solvedIn by remember(puzzle) { mutableStateOf<Int?>(null) }
    // A drag paints every square it crosses the same way as the first one it touched.
    var paint by remember { mutableStateOf(1.0) }
    val shake = remember { Animatable(0f) }
    val pop = popScale(solvedIn)

    fun cellAt(at: Offset, width: Int): Int {
        val cell = width / GridMath.N.toFloat()
        val col = (at.x / cell).toInt().coerceIn(0, GridMath.N - 1)
        val row = (at.y / cell).toInt().coerceIn(0, GridMath.N - 1)
        return row * GridMath.N + col
    }

    fun put(i: Int, value: Double) {
        if (solvedIn != null || puzzle.isGiven(i) || cells[i] == value) return
        cells[i] = value
        flagged.remove(i)
        off = null
        haptics.tick()
    }

    Box(Modifier.fillMaxSize()) {
        PuzzleFrame(
            title = stringResource(R.string.build_title, index + 1, DailySet.BUILDS),
            level = set.level,
            clock = clock,
            solved = solvedIn != null,
            progress = dayProgress(set),
            step = stepOf(Kind.BUILD, index),
            onBack = onBack,
        ) {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
                Text(puzzle.rule.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(R.string.build_help),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
                GridView(
                    cells.toDoubleArray(),
                    ink = if (solvedIn != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    given = puzzle::isGiven,
                    flagged = flagged.toSet(),
                    guide = if (solvedIn == null) puzzle.rule else null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp, horizontal = 8.dp)
                        .offset { IntOffset(shake.value.roundToInt(), 0) }
                        .scale(pop)
                        .pointerInput(puzzle) {
                            detectTapGestures { at ->
                                val i = cellAt(at, size.width)
                                put(i, 1.0 - cells[i])
                            }
                        }
                        .pointerInput(puzzle) {
                            detectDragGestures(
                                onDragStart = { at ->
                                    val i = cellAt(at, size.width)
                                    paint = 1.0 - cells[i]
                                    put(i, paint)
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    put(cellAt(change.position, size.width), paint)
                                },
                            )
                        },
                )
                off?.let {
                    Text(
                        pluralStringResource(R.plurals.build_off, it, it),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            if (solvedIn == null) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Spacer(Modifier.weight(1f))
                    Button(onClick = {
                        tries++
                        val now = cells.toDoubleArray()
                        val wrong = LumeCore.active.mismatch(now, puzzle.answer).toInt()
                        if (wrong == 0) {
                            val seconds = clock.stop()
                            solvedIn = seconds
                            flagged.clear()
                            haptics.solved()
                            store.record(set.date, Result(DailySet.id(Kind.BUILD, index), seconds, tries = tries))
                        } else {
                            off = wrong
                            flagged.clear()
                            flagged.addAll(now.indices.filter { now[it] != puzzle.answer[it] })
                            haptics.nudge()
                            scope.launch {
                                for (x in listOf(14f, -12f, 8f, -5f, 2f, 0f)) shake.animateTo(x, tween(45))
                            }
                        }
                    }) { Text(stringResource(R.string.build_check)) }
                }
            }
            SolvedPanel(solvedIn != null, solvedIn ?: 0, note = null, onNext = onNext)
        }
        Confetti(solvedIn != null)
    }
}
