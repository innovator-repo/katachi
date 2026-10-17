package app.katachi.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.katachi.R
import app.katachi.core.GridMath
import kotlinx.coroutines.delay

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

/** The frame every puzzle sits in: back, title, a running clock, and the solved panel at the bottom. */
@Composable
fun PuzzleFrame(
    title: String,
    clock: Clock,
    solved: Boolean,
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
            Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            Text(
                formatSeconds(clock.seconds(clock.stoppedAt ?: now)),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 16.dp),
            )
        }
        content()
    }
}

/** Shown under a solved puzzle: what it was about, and the way on. */
@Composable
fun SolvedPanel(seconds: Int, note: String?, onNext: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth().padding(16.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.solved_in, formatSeconds(seconds)), style = MaterialTheme.typography.titleMedium)
            if (note != null) Text(note, style = MaterialTheme.typography.bodyMedium)
            Row {
                Spacer(Modifier.weight(1f))
                Button(onClick = onNext) { Text(stringResource(R.string.next)) }
            }
        }
    }
}

/** A 6 × 6 pattern. [given] marks cells drawn on a tinted ground (fixed cells in pattern building). */
@Composable
fun GridView(
    grid: DoubleArray,
    modifier: Modifier = Modifier,
    ink: Color = MaterialTheme.colorScheme.onSurface,
    given: ((Int) -> Boolean)? = null,
) {
    val line = MaterialTheme.colorScheme.outline
    val tint = MaterialTheme.colorScheme.surfaceVariant
    Canvas(modifier.aspectRatio(1f)) {
        val n = GridMath.N
        val cell = size.width / n
        for (i in 0 until GridMath.CELLS) {
            val at = Offset((i % n) * cell, (i / n) * cell)
            if (given?.invoke(i) == true) drawRect(tint, at, Size(cell, cell))
            if (grid[i] == 1.0) {
                val inset = cell * 0.06f
                drawRect(ink, at + Offset(inset, inset), Size(cell - 2 * inset, cell - 2 * inset))
            }
        }
        val stroke = 1.dp.toPx()
        for (k in 0..n) {
            drawLine(line, Offset(k * cell, 0f), Offset(k * cell, size.height), stroke)
            drawLine(line, Offset(0f, k * cell), Offset(size.width, k * cell), stroke)
        }
        drawRect(line, style = Stroke(stroke * 1.5f))
    }
}
