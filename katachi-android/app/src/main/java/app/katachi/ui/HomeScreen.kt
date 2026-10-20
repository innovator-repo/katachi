package app.katachi.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import app.katachi.core.Difficulty
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.katachi.LumeCore
import app.katachi.R
import app.katachi.core.DailySet
import app.katachi.core.DailyTarget
import app.katachi.designsystem.GroupedList
import app.katachi.designsystem.GroupedRow
import app.katachi.lumeState
import app.katachi.core.Day
import app.katachi.core.Kind
import app.katachi.designsystem.LumeCard
import app.katachi.core.Tangram
import app.katachi.store
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun HomeScreen(set: DailySet, onOpen: (Kind, Int) -> Unit) {
    val history by LocalContext.current.store.history.collectAsState()
    val day = history.day(set.date)
    val solved = day.results.size
    val context = LocalContext.current
    val floorDate by context.lumeState.floorOn.collectAsState()
    val floor = floorDate == set.date.toString()
    val target = DailyTarget.of(floor)
    Column(
        Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.app_name_full), style = MaterialTheme.typography.headlineMedium)
                Text(
                    set.date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.padding(top = 8.dp),
                ) {
                    Text(
                        stringResource(R.string.level, set.level),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    )
                }
            }
            if (floor) ProgressRing(minOf(solved, target), target) else ProgressRing(solved, DailySet.TOTAL)
        }
        if (floor) {
            Text(
                stringResource(R.string.home_floor),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Text(
            when {
                set.level >= Difficulty.MAX -> stringResource(R.string.home_level_top)
                solved >= Difficulty.TO_LEVEL_UP -> stringResource(R.string.home_level_won, set.level + 1)
                else -> (Difficulty.TO_LEVEL_UP - solved).let {
                    pluralStringResource(R.plurals.home_level_next, it, it, set.level + 1)
                }
            },
            style = MaterialTheme.typography.bodyMedium,
        )
        val upcoming = DailySet.order.firstOrNull { (kind, i) -> !day.solved(DailySet.id(kind, i)) }
        if (floor && solved >= target) {
            Text(stringResource(R.string.home_floor_done), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        } else if (upcoming != null) {
            Button(onClick = { onOpen(upcoming.first, upcoming.second) }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                Text(
                    stringResource(if (solved == 0) R.string.home_start else R.string.home_continue),
                    Modifier.padding(start = 6.dp),
                )
            }
        } else {
            Text(stringResource(R.string.home_done), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        }
        val streak = history.streak(set.date)
        val pace = history.tangramPace(set.date)
        val before = history.tangramPace(set.date.minusDays(7))
        if (streak > 0 || pace != null) {
            GroupedList {
                var first = true
                if (streak > 0) {
                    GroupedRow(stringResource(R.string.row_streak), pluralStringResource(R.plurals.home_streak, streak, streak), first = true)
                    first = false
                }
                if (pace != null) {
                    GroupedRow(stringResource(R.string.row_pace), formatSeconds(pace.roundToInt()), first = first)
                    if (before != null) GroupedRow(stringResource(R.string.row_pace_before), formatSeconds(before.roundToInt()))
                }
            }
        }

        Section(stringResource(R.string.section_tangram), stringResource(R.string.section_tangram_hint))
        TileRow(DailySet.TANGRAMS, Kind.TANGRAM, day, onOpen) { i -> Silhouette(set.tangrams[i].tangram) }

        Section(stringResource(R.string.section_matrix), stringResource(R.string.section_matrix_hint))
        TileRow(DailySet.MATRICES, Kind.MATRIX, day, onOpen) { i -> GridView(set.matrices[i].cells[0], Modifier.padding(4.dp)) }

        Section(stringResource(R.string.section_build), stringResource(R.string.section_build_hint))
        TileRow(DailySet.BUILDS, Kind.BUILD, day, onOpen) { i ->
            GridView(set.builds[i].given, Modifier.padding(4.dp), given = set.builds[i]::isGiven)
        }

        Text(
            stringResource(if (LumeCore.available) R.string.engine_lume else R.string.engine_kotlin),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

/** How much of the day is done, as a ring that fills as puzzles are solved. */
@Composable
private fun ProgressRing(solved: Int, total: Int) {
    val sweep = animatedFraction(solved / total.toFloat())
    val track = MaterialTheme.colorScheme.surfaceVariant
    val fill = MaterialTheme.colorScheme.primary
    Box(Modifier.size(84.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val width = 8.dp.toPx()
            val inset = Offset(width / 2, width / 2)
            val arc = Size(size.width - width, size.height - width)
            drawArc(track, 0f, 360f, false, inset, arc, style = Stroke(width))
            drawArc(fill, -90f, 360f * sweep, false, inset, arc, style = Stroke(width, cap = StrokeCap.Round))
        }
        Text(stringResource(R.string.home_ring, solved, total), style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun Section(title: String, hint: String) {
    Column(Modifier.padding(top = 8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Five slots a row, so tangrams and the shorter rows line up. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TileRow(count: Int, kind: Kind, day: Day, onOpen: (Kind, Int) -> Unit, preview: @Composable (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        for (i in 0 until DailySet.TANGRAMS) {
            if (i >= count) {
                Box(Modifier.weight(1f))
                continue
            }
            val result = day.results.firstOrNull { it.id == DailySet.id(kind, i) }
            LumeCard(
                onClick = { onOpen(kind, i) },
                border = BorderStroke(
                    if (result != null) 2.dp else 1.dp,
                    if (result != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                ),
                modifier = Modifier.weight(1f),
            ) {
                Column {
                    Box(Modifier.fillMaxWidth().aspectRatio(1f).padding(4.dp)) {
                        preview(i)
                        if (result != null) {
                            val pop = popScale(result.id)
                            Icon(
                                Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.align(Alignment.TopEnd).size(18.dp).scale(pop),
                            )
                        }
                    }
                    Text(
                        result?.let { formatSeconds(it.seconds) } ?: "${i + 1}",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 6.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun Silhouette(tangram: Tangram) {
    val ink = MaterialTheme.colorScheme.onSurface
    Canvas(Modifier.fillMaxSize().padding(4.dp)) {
        val scale = (size.minDimension / max(tangram.width, tangram.height)).toFloat()
        val left = (size.width - tangram.width.toFloat() * scale) / 2
        val top = (size.height - tangram.height.toFloat() * scale) / 2
        for (piece in tangram.pieces) {
            val pts = piece.polygon.map { Offset(left + it.x.toFloat() * scale, top + it.y.toFloat() * scale) }
            val path = Path().apply {
                moveTo(pts[0].x, pts[0].y)
                pts.drop(1).forEach { lineTo(it.x, it.y) }
                close()
            }
            drawPath(path, ink)
        }
    }
}
