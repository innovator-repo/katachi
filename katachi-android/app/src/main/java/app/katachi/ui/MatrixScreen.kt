package app.katachi.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import app.katachi.R
import app.katachi.core.DailySet
import app.katachi.core.Kind
import app.katachi.designsystem.LumeCard
import app.katachi.core.Result
import app.katachi.store
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatrixScreen(set: DailySet, index: Int, onBack: () -> Unit, onNext: () -> Unit) {
    val puzzle = set.matrices[index]
    val store = LocalContext.current.store
    val haptics = rememberHaptics()
    val scope = rememberCoroutineScope()
    val clock = remember(puzzle) { Clock() }
    val wrong = remember(puzzle) { mutableStateListOf<Int>() }
    var solvedIn by remember(puzzle) { mutableStateOf<Int?>(null) }
    var shaking by remember(puzzle) { mutableIntStateOf(-1) }
    val shake = remember { Animatable(0f) }
    val pop = popScale(solvedIn)

    Box(Modifier.fillMaxSize()) {
        PuzzleFrame(
            title = stringResource(R.string.matrix_title, index + 1, DailySet.MATRICES),
            level = set.level,
            clock = clock,
            solved = solvedIn != null,
            progress = dayProgress(set),
            step = stepOf(Kind.MATRIX, index),
            onBack = onBack,
            attempt = if (solvedIn == null) (wrong.size * 0.25f).coerceAtMost(0.75f) else 0f,
        ) {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
                Text(
                    stringResource(R.string.matrix_help),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Column(
                    Modifier.fillMaxWidth().padding(vertical = 12.dp).padding(horizontal = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    for (row in 0 until 3) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            for (col in 0 until 3) {
                                val i = row * 3 + col
                                Box(Modifier.weight(1f).aspectRatio(1f), contentAlignment = Alignment.Center) {
                                    if (i < 8) {
                                        // After a miss, the two grids to compare in the gap's row glow jade.
                                        val lit = wrong.isNotEmpty() && solvedIn == null && i in 6..7
                                        GridView(puzzle.cells[i], if (lit) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(6.dp)) else Modifier)
                                    } else {
                                        AnimatedContent(
                                            solvedIn != null,
                                            transitionSpec = { (fadeIn(tween(250)) + scaleIn(initialScale = 0.6f)) togetherWith fadeOut(tween(150)) },
                                            label = "gap",
                                        ) { filled ->
                                            if (filled) {
                                                GridView(puzzle.cells[8], Modifier.scale(pop), ink = MaterialTheme.colorScheme.primary)
                                            } else {
                                                LumeCard(Modifier.fillMaxWidth().aspectRatio(1f)) {
                                                    Box(Modifier.fillMaxWidth().aspectRatio(1f), contentAlignment = Alignment.Center) {
                                                        Text("?", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                Text(stringResource(R.string.matrix_pick), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(vertical = 8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    puzzle.options.forEachIndexed { i, option ->
                        val isWrong = i in wrong
                        val isRight = solvedIn != null && i == puzzle.answer
                        val dim = animatedFraction(if (isWrong || (solvedIn != null && !isRight)) 0.4f else 1f)
                        LumeCard(
                            onClick = {
                                if (solvedIn != null || isWrong) return@LumeCard
                                if (i == puzzle.answer) {
                                    val seconds = clock.stop()
                                    solvedIn = seconds
                                    haptics.solved()
                                    store.record(set.date, Result(DailySet.id(Kind.MATRIX, index), seconds, tries = wrong.size + 1))
                                } else {
                                    wrong += i
                                    shaking = i
                                    haptics.nudge()
                                    scope.launch {
                                        for (x in listOf(6f, -5f, 3f, 0f)) shake.animateTo(x, tween(60))
                                    }
                                }
                            },
                            border = BorderStroke(
                                if (isRight || isWrong) 2.dp else 1.dp,
                                when {
                                    isRight -> MaterialTheme.colorScheme.primary
                                    isWrong -> MaterialTheme.colorScheme.outline
                                    else -> MaterialTheme.colorScheme.outline
                                },
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("option$i")
                                .offset { IntOffset(if (i == shaking) shake.value.roundToInt() else 0, 0) }
                                .scale(if (isRight) pop else 1f)
                                .alpha(dim),
                        ) {
                            GridView(option, Modifier.padding(6.dp))
                        }
                    }
                }
                if (wrong.isNotEmpty() && solvedIn == null) {
                    Text(
                        stringResource(R.string.matrix_wrong),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
            SolvedPanel(solvedIn != null, solvedIn ?: 0, note = puzzle.rule, onNext = onNext)
        }
        Confetti(solvedIn != null)
    }
}
