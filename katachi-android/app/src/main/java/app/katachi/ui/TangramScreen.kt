package app.katachi.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Flip
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import app.katachi.LumeCore
import app.katachi.R
import app.katachi.core.DailySet
import app.katachi.core.Geometry
import app.katachi.core.Kind
import app.katachi.core.Placed
import app.katachi.core.Pose
import app.katachi.core.Raster
import app.katachi.core.Result
import app.katachi.core.Shape
import app.katachi.core.Snap
import app.katachi.core.Tangram
import app.katachi.core.Vec
import app.katachi.store
import kotlin.math.max
import kotlin.math.min

/** One colour per piece, in the order of [Shape.SET]. */
private val pieceColors = listOf(
    Color(0xFFE0614F), Color(0xFF3F8FD2), Color(0xFFF2B33D), Color(0xFF8E6BC8),
    Color(0xFF4DB38E), Color(0xFFE58AB4), Color(0xFF53B8C9),
)

/** Where each piece waits before it is used: two rows under the silhouette, centred on it. */
private fun trayPoses(tangram: Tangram): List<Pose> {
    val cx = tangram.width / 2
    val top = tangram.height + 1.2
    val spots = listOf(
        Vec(cx - 2.6, top + 0.7), Vec(cx, top + 0.7), Vec(cx + 2.6, top + 0.5),
        Vec(cx - 2.9, top + 2.4), Vec(cx - 1.5, top + 2.4), Vec(cx + 0.1, top + 2.4), Vec(cx + 2.2, top + 2.4),
    )
    return spots.map { Pose(at = it) }
}

/** World units to pixels: the silhouette, the tray and a margin, fitted to the canvas. */
private class Frame(tangram: Tangram, size: IntSize) {
    private val minX = min(-0.4, tangram.width / 2 - 4.0)
    private val maxX = max(tangram.width + 0.4, tangram.width / 2 + 4.0)
    private val minY = -0.4
    private val maxY = tangram.height + 4.4
    val scale = min(size.width / (maxX - minX), size.height / (maxY - minY)).toFloat()
    private val left = (size.width - (maxX - minX) * scale) / 2 - minX * scale
    private val top = (size.height - (maxY - minY) * scale) / 2 - minY * scale

    fun toScreen(v: Vec) = Offset((left + v.x * scale).toFloat(), (top + v.y * scale).toFloat())
    fun toWorld(o: Offset) = Vec((o.x - left) / scale, (o.y - top) / scale)
}

@Composable
fun TangramScreen(set: DailySet, index: Int, onBack: () -> Unit, onNext: () -> Unit) {
    val puzzle = set.tangrams[index]
    val tangram = puzzle.tangram
    val id = DailySet.id(Kind.TANGRAM, index)
    val store = LocalContext.current.store
    val math = LumeCore.active
    val raster = remember(tangram) { Raster(tangram) }
    val clock = remember(tangram) { Clock() }

    // Draw order is list order; the piece last touched moves to the end so it sits on top.
    val pieces = remember(tangram) {
        mutableStateListOf<Placed>().apply { addAll(Shape.SET.zip(trayPoses(tangram)) { s, p -> Placed(s, p) }) }
    }
    val colorOf = remember(tangram) { mutableStateListOf<Int>().apply { addAll(Shape.SET.indices) } }
    var outlines by remember(tangram) { mutableIntStateOf(puzzle.outlinesShown) }
    var solvedIn by remember(tangram) { mutableStateOf<Int?>(null) }
    var size by remember { mutableStateOf(IntSize.Zero) }
    var dragging by remember { mutableIntStateOf(-1) }

    fun frame() = Frame(tangram, size)

    fun check() {
        if (solvedIn != null) return
        val error = math.tangramError(raster.target, raster.cover(pieces.map { it.polygon }))
        if (error < Raster.SOLVED) {
            val seconds = clock.stop()
            solvedIn = seconds
            store.record(set.date, Result(id, seconds, hints = outlines - puzzle.outlinesShown))
        }
    }

    fun settle(i: Int) {
        val others = pieces.filterIndexed { j, _ -> j != i }.flatMap { it.polygon }
        pieces[i] = Snap.snap(pieces[i], tangram.vertices + others)
        check()
    }

    fun raise(i: Int): Int {
        val last = pieces.lastIndex
        if (i == last) return i
        pieces.add(pieces.removeAt(i))
        colorOf.add(colorOf.removeAt(i))
        return last
    }

    fun hit(at: Offset): Int {
        val p = frame().toWorld(at)
        return pieces.indices.lastOrNull { Geometry.contains(pieces[it].polygon, p) } ?: -1
    }

    // Opaque, so the seams where answer pieces meet don't show through as darker lines.
    val silhouette = lerp(MaterialTheme.colorScheme.background, MaterialTheme.colorScheme.onSurface, 0.82f)
    val hintLine = MaterialTheme.colorScheme.surface
    val edge = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)

    PuzzleFrame(
        title = stringResource(R.string.tangram_title, index + 1, DailySet.TANGRAMS),
        clock = clock,
        solved = solvedIn != null,
        onBack = onBack,
    ) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .onSizeChanged { size = it }
                .pointerInput(tangram) {
                    detectTapGestures(onTap = { at ->
                        val i = hit(at)
                        if (i >= 0 && solvedIn == null) {
                            val top = raise(i)
                            val p = pieces[top]
                            pieces[top] = p.copy(pose = p.pose.copy(turn = (p.pose.turn + 1) % 8))
                            settle(top)
                        }
                    })
                }
                .pointerInput(tangram) {
                    detectDragGestures(
                        onDragStart = { at ->
                            val i = hit(at)
                            dragging = if (i >= 0 && solvedIn == null) raise(i) else -1
                        },
                        onDrag = { change, amount ->
                            if (dragging >= 0) {
                                change.consume()
                                val p = pieces[dragging]
                                val scale = frame().scale
                                pieces[dragging] = p.copy(pose = p.pose.copy(at = p.pose.at + Vec(amount.x / scale.toDouble(), amount.y / scale.toDouble())))
                            }
                        },
                        onDragEnd = { if (dragging >= 0) settle(dragging); dragging = -1 },
                        onDragCancel = { dragging = -1 },
                    )
                },
        ) {
            if (size == IntSize.Zero) return@Canvas
            val f = frame()
            // The silhouette: every piece of the answer in one colour, edged in the same colour to hide the seams.
            for (piece in tangram.pieces) {
                val path = piece.polygon.toPath(f)
                drawPath(path, silhouette)
                drawPath(path, silhouette, style = Stroke(1.5f, join = StrokeJoin.Round))
            }
            for (piece in tangram.pieces.take(outlines)) {
                drawPath(
                    piece.polygon.toPath(f), hintLine,
                    style = Stroke(2.dp.toPx(), join = StrokeJoin.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f))),
                )
            }
            pieces.forEachIndexed { i, piece -> drawPiece(piece.polygon.toPath(f), pieceColors[colorOf[i]], edge) }
        }
        if (solvedIn != null) {
            SolvedPanel(solvedIn!!, note = null, onNext = onNext)
        } else {
            Text(
                stringResource(R.string.tangram_help),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {
                    val i = pieces.indexOfFirst { it.shape.canFlip }
                    val p = pieces[i]
                    pieces[i] = p.copy(pose = p.pose.copy(flipped = !p.pose.flipped))
                    settle(i)
                }) {
                    Icon(Icons.Outlined.Flip, contentDescription = null)
                    Text(stringResource(R.string.tangram_flip), Modifier.padding(start = 6.dp))
                }
                OutlinedButton(
                    onClick = { outlines++ },
                    enabled = outlines < tangram.pieces.size,
                ) {
                    Icon(Icons.Outlined.Lightbulb, contentDescription = null)
                    Text(stringResource(R.string.tangram_hint), Modifier.padding(start = 6.dp))
                }
                OutlinedButton(onClick = {
                    val home = trayPoses(tangram)
                    for (i in pieces.indices) pieces[i] = pieces[i].copy(pose = home[colorOf[i]])
                }) {
                    Icon(Icons.Outlined.RestartAlt, contentDescription = stringResource(R.string.tangram_reset))
                }
            }
        }
    }
}

private fun List<Vec>.toPath(f: Frame) = Path().apply {
    val start = f.toScreen(first())
    moveTo(start.x, start.y)
    for (v in drop(1)) f.toScreen(v).let { lineTo(it.x, it.y) }
    close()
}

private fun DrawScope.drawPiece(path: Path, fill: Color, edge: Color) {
    drawPath(path, fill.copy(alpha = 0.92f))
    drawPath(path, edge, style = Stroke(1.5.dp.toPx(), join = StrokeJoin.Round))
}
