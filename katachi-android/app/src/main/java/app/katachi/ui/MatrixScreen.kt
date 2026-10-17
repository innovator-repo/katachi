package app.katachi.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.katachi.R
import app.katachi.core.DailySet
import app.katachi.core.Kind
import app.katachi.core.Result
import app.katachi.store

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatrixScreen(set: DailySet, index: Int, onBack: () -> Unit, onNext: () -> Unit) {
    val puzzle = set.matrices[index]
    val store = LocalContext.current.store
    val clock = remember(puzzle) { Clock() }
    val wrong = remember(puzzle) { mutableStateListOf<Int>() }
    var solvedIn by remember(puzzle) { mutableStateOf<Int?>(null) }

    PuzzleFrame(
        title = stringResource(R.string.matrix_title, index + 1, DailySet.MATRICES),
        clock = clock,
        solved = solvedIn != null,
        onBack = onBack,
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
                                if (i < 8 || solvedIn != null) {
                                    GridView(puzzle.cells[i], ink = if (i == 8) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                                } else {
                                    OutlinedCard(Modifier.fillMaxWidth().aspectRatio(1f)) {
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
            Text(stringResource(R.string.matrix_pick), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(vertical = 8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                puzzle.options.forEachIndexed { i, option ->
                    val isWrong = i in wrong
                    val isRight = solvedIn != null && i == puzzle.answer
                    OutlinedCard(
                        onClick = {
                            if (solvedIn != null || isWrong) return@OutlinedCard
                            if (i == puzzle.answer) {
                                val seconds = clock.stop()
                                solvedIn = seconds
                                store.record(set.date, Result(DailySet.id(Kind.MATRIX, index), seconds, tries = wrong.size + 1))
                            } else {
                                wrong += i
                            }
                        },
                        border = BorderStroke(
                            if (isRight || isWrong) 2.dp else 1.dp,
                            when {
                                isRight -> MaterialTheme.colorScheme.primary
                                isWrong -> MaterialTheme.colorScheme.error
                                else -> MaterialTheme.colorScheme.outline
                            },
                        ),
                        modifier = Modifier.weight(1f).alpha(if (isWrong) 0.45f else 1f),
                    ) {
                        GridView(option, Modifier.padding(6.dp))
                    }
                }
            }
            if (wrong.isNotEmpty() && solvedIn == null) {
                Text(
                    stringResource(R.string.matrix_wrong),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
        solvedIn?.let { SolvedPanel(it, note = puzzle.rule, onNext = onNext) }
    }
}
