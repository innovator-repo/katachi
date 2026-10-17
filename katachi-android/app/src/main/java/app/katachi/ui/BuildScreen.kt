package app.katachi.ui

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.katachi.LumeCore
import app.katachi.R
import app.katachi.core.DailySet
import app.katachi.core.GridMath
import app.katachi.core.Kind
import app.katachi.core.Result
import app.katachi.store

@Composable
fun BuildScreen(set: DailySet, index: Int, onBack: () -> Unit, onNext: () -> Unit) {
    val puzzle = set.builds[index]
    val store = LocalContext.current.store
    val clock = remember(puzzle) { Clock() }
    val cells = remember(puzzle) { mutableStateListOf<Double>().apply { addAll(puzzle.given.toList()) } }
    var tries by remember(puzzle) { mutableIntStateOf(0) }
    var off by remember(puzzle) { mutableStateOf<Int?>(null) }
    var solvedIn by remember(puzzle) { mutableStateOf<Int?>(null) }

    PuzzleFrame(
        title = stringResource(R.string.build_title, index + 1, DailySet.BUILDS),
        clock = clock,
        solved = solvedIn != null,
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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp, horizontal = 8.dp)
                    .pointerInput(puzzle) {
                        detectTapGestures { at ->
                            if (solvedIn != null) return@detectTapGestures
                            val cell = size.width / GridMath.N.toFloat()
                            val col = (at.x / cell).toInt().coerceIn(0, GridMath.N - 1)
                            val row = (at.y / cell).toInt().coerceIn(0, GridMath.N - 1)
                            val i = row * GridMath.N + col
                            if (!puzzle.isGiven(i)) {
                                cells[i] = 1.0 - cells[i]
                                off = null
                            }
                        }
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
        if (solvedIn != null) {
            SolvedPanel(solvedIn!!, note = null, onNext = onNext)
        } else {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.weight(1f))
                Button(onClick = {
                    tries++
                    val wrong = LumeCore.active.mismatch(cells.toDoubleArray(), puzzle.answer).toInt()
                    if (wrong == 0) {
                        val seconds = clock.stop()
                        solvedIn = seconds
                        store.record(set.date, Result(DailySet.id(Kind.BUILD, index), seconds, tries = tries))
                    } else {
                        off = wrong
                    }
                }) { Text(stringResource(R.string.build_check)) }
            }
        }
    }
}
