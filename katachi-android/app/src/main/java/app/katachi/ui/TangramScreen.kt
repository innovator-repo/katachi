package app.katachi.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Flip
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
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
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

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

/** Share of the silhouette that pieces cover, from 0 to 1; overlaps and overhangs don't count. */
private fun coverage(target: DoubleArray, cover: DoubleArray): Float {
    var inside = 0.0
    var total = 0.0
    for (i in target.indices) {
        total += target[i]
        inside += min(target[i], cover[i])
    }
    return if (total == 0.0) 0f else (inside / total).toFloat()
}

@Composable
fun TangramScreen(set: DailySet, index: Int, onBack: () -> Unit, onNext: () -> Unit) {
    val puzzle = set.tangrams[index]
    val tangram = puzzle.tangram
    val id = DailySet.id(Kind.TANGRAM, index)
    val store = LocalContext.current.store
    val math = LumeCore.active
    val haptics = rememberHaptics()
    val scope = rememberCoroutineScope()
    val raster = remember(tangram) { Raster(tangram) }
    val clock = remember(tangram) { Clock() }

    // Draw order is list order; the piece last touched moves to the end so it sits on top.
    val pieces = remember(tangram) {
        mutableStateListOf<Placed>().apply { addAll(Shape.SET.zip(trayPoses(tangram)) { s, p -> Placed(s, p) }) }
    }
    val colorOf = remember(tangram) { mutableStateListOf<Int>().apply { addAll(Shape.SET.indices) } }
    var outlines by remember(tangram) { mutableIntStateOf(puzzle.outlinesShown) }
    var solvedIn by remember(tangram) { mutableStateOf<Int?>(null) }
    var covered by remember(tangram) { mutableStateOf(0f) }
    var size by remember { mutableStateOf(IntSize.Zero) }
    var dragging by remember { mutableIntStateOf(-1) }

    // The last piece moved eases from where it was to where it landed: a short slide and, after a tap, a turn.
    var moving by remember(tangram) { mutableIntStateOf(-1) }
    val slide = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    val spin = remember { Animatable(0f) }
    val lift = remember { Animatable(1f) }

    fun frame() = Frame(tangram, size)

    fun check() {
        if (solvedIn != null) return
        val cover = raster.cover(pieces.map { it.polygon })
        covered = coverage(raster.target, cover)
        val error = math.tangramError(raster.target, cover)
        if (error < Raster.SOLVED) {
            val seconds = clock.stop()
            solvedIn = seconds
            haptics.solved()
            store.record(set.date, Result(id, seconds, hints = outlines - puzzle.outlinesShown))
        }
    }

    /** Snaps piece [i] from [before] and animates the move; turned is true when it was just rotated. */
    fun settle(i: Int, before: Placed, turned: Boolean = false) {
        val others = pieces.filterIndexed { j, _ -> j != i }.flatMap { it.polygon }
        val loose = pieces[i]
        val snapped = Snap.snap(loose, tangram.vertices + others)
        pieces[i] = snapped
        if (snapped.pose.at != loose.pose.at) haptics.tick()
        moving = colorOf[i]
        val scale = if (size == IntSize.Zero) 0f else frame().scale
        val gap = before.pose.at - snapped.pose.at
        scope.launch {
            slide.snapTo(Offset(gap.x.toFloat() * scale, gap.y.toFloat() * scale))
            slide.animateTo(Offset.Zero, spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow))
        }
        scope.launch {
            spin.snapTo(if (turned) -45f else 0f)
            spin.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow))
        }
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
    val solvedFill = MaterialTheme.colorScheme.primary
    val hintLine = MaterialTheme.colorScheme.surface
    val edge = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
    val shadow = Color.Black.copy(alpha = 0.22f)
    val glow = animatedFraction(if (solvedIn != null) 1f else 0f)

    Box(Modifier.fillMaxSize()) {
        PuzzleFrame(
            title = stringResource(R.string.tangram_title, index + 1, DailySet.TANGRAMS),
            level = set.level,
            clock = clock,
            solved = solvedIn != null,
            progress = dayProgress(set),
            step = stepOf(Kind.TANGRAM, index),
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
                                settle(top, p, turned = true)
                            }
                        })
                    }
                    .pointerInput(tangram) {
                        detectDragGestures(
                            onDragStart = { at ->
                                val i = hit(at)
                                dragging = if (i >= 0 && solvedIn == null) raise(i) else -1
                                if (dragging >= 0) {
                                    moving = -1
                                    scope.launch { lift.animateTo(1.06f, spring(stiffness = Spring.StiffnessHigh)) }
                                }
                            },
                            onDrag = { change, amount ->
                                if (dragging >= 0) {
                                    change.consume()
                                    val p = pieces[dragging]
                                    val scale = frame().scale
                                    pieces[dragging] = p.copy(pose = p.pose.copy(at = p.pose.at + Vec(amount.x / scale.toDouble(), amount.y / scale.toDouble())))
                                }
                            },
                            onDragEnd = {
                                if (dragging >= 0) {
                                    val i = dragging
                                    dragging = -1
                                    scope.launch { lift.animateTo(1f, spring(stiffness = Spring.StiffnessHigh)) }
                                    settle(i, pieces[i])
                                }
                            },
                            onDragCancel = {
                                dragging = -1
                                scope.launch { lift.snapTo(1f) }
                            },
                        )
                    },
            ) {
                if (size == IntSize.Zero) return@Canvas
                val f = frame()
                // The silhouette: every piece of the answer in one colour, edged in the same colour to hide the seams.
                val ground = lerp(silhouette, solvedFill, glow * 0.6f)
                for (piece in tangram.pieces) {
                    val path = piece.polygon.toPath(f)
                    drawPath(path, ground)
                    drawPath(path, ground, style = Stroke(1.5f, join = StrokeJoin.Round))
                }
                for (piece in tangram.pieces.take(outlines)) {
                    drawPath(
                        piece.polygon.toPath(f), hintLine,
                        style = Stroke(2.dp.toPx(), join = StrokeJoin.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f))),
                    )
                }
                pieces.forEachIndexed { i, piece ->
                    val path = piece.polygon.toPath(f)
                    val centre = f.toScreen(piece.pose.at)
                    val fill = pieceColors[colorOf[i]]
                    when {
                        i == dragging -> {
                            translate(0f, 6.dp.toPx()) { scale(lift.value, centre) { drawPath(path, shadow) } }
                            scale(lift.value, centre) { drawPiece(path, fill, edge) }
                        }
                        colorOf[i] == moving -> translate(slide.value.x, slide.value.y) {
                            rotate(spin.value, centre) { drawPiece(path, fill, edge) }
                        }
                        else -> drawPiece(path, fill, edge)
                    }
                }
            }
            if (solvedIn == null) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    LinearProgressIndicator(progress = animatedFraction(covered), modifier = Modifier.weight(1f))
                    Text(
                        stringResource(R.string.tangram_cover, (covered * 100).roundToInt()),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
                Text(
                    stringResource(R.string.tangram_help),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
                Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = {
                        val i = pieces.indexOfFirst { it.shape.canFlip }
                        val p = pieces[i]
                        pieces[i] = p.copy(pose = p.pose.copy(flipped = !p.pose.flipped))
                        settle(i, p)
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
                        moving = -1
                        for (i in pieces.indices) pieces[i] = pieces[i].copy(pose = home[colorOf[i]])
                        covered = 0f
                    }) {
                        Icon(Icons.Outlined.RestartAlt, contentDescription = stringResource(R.string.tangram_reset))
                    }
                }
            }
            SolvedPanel(solvedIn != null, solvedIn ?: 0, note = null, onNext = onNext)
        }
        Confetti(solvedIn != null)
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
