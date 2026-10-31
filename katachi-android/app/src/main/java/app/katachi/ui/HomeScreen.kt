package app.katachi.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.katachi.R
import app.katachi.core.DailySet
import app.katachi.core.Kind
import app.katachi.core.Tangram
import app.katachi.core.bestSeconds
import app.katachi.core.nextNumber
import app.katachi.core.nextUnsolved
import app.katachi.core.solvedOf
import app.katachi.designsystem.LumeType
import app.katachi.designsystem.pressable
import app.katachi.store
import kotlinx.coroutines.launch
import kotlin.math.max

/**
 * The one main screen: a small header, the hero (the next puzzle and a Solve button) and a pull-up
 * "Today" drawer with the three puzzle types and the Stats and Streak shortcuts.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    set: DailySet,
    onOpen: (Kind, Int) -> Unit,
    onStats: () -> Unit = {},
    onStreak: () -> Unit = {},
    onSettings: () -> Unit = {},
) {
    val context = LocalContext.current
    val history by context.store.history.collectAsState()
    val day = history.day(set.date)
    val solved = day.results.size
    val streak = history.streak(set.date)
    val next = day.nextUnsolved()
    val sheet = rememberStandardBottomSheetState(initialValue = SheetValue.PartiallyExpanded, skipHiddenState = true)
    val scaffold = rememberBottomSheetScaffoldState(sheet)
    val scope = rememberCoroutineScope()

    BottomSheetScaffold(
        scaffoldState = scaffold,
        sheetPeekHeight = PEEK,
        containerColor = MaterialTheme.colorScheme.background,
        sheetContainerColor = MaterialTheme.colorScheme.surface,
        sheetShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        sheetTonalElevation = 0.dp,
        sheetShadowElevation = 8.dp,
        sheetDragHandle = {
            Box(
                Modifier
                    .heightIn(min = 48.dp)
                    .fillMaxWidth()
                    .clickable {
                        scope.launch { if (sheet.currentValue == SheetValue.Expanded) sheet.partialExpand() else sheet.expand() }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(width = 36.dp, height = 4.dp),
                ) {}
            }
        },
        sheetContent = {
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.drawer_title), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    Text(
                        stringResource(R.string.drawer_summary, solved, DailySet.TOTAL),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Chip(stringResource(R.string.chip_stats), onStats)
                    Chip(if (streak > 0) stringResource(R.string.chip_streak_n, streak) else stringResource(R.string.chip_streak), onStreak)
                }
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                ) {
                    for ((i, kind) in Kind.entries.withIndex()) {
                        if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
                        val firstOpen = (0 until DailySet.count(kind)).firstOrNull { !day.solved(DailySet.id(kind, it)) }
                        DrawerRow(
                            title = stringResource(kindTitleRes(kind)),
                            value = stringResource(R.string.drawer_count, day.solvedOf(kind), DailySet.count(kind)),
                            onClick = firstOpen?.let { index -> { onOpen(kind, index) } },
                        )
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
                    DrawerRow(
                        title = stringResource(R.string.drawer_best),
                        value = day.bestSeconds()?.let { formatSeconds(it) } ?: stringResource(R.string.none_yet),
                        onClick = null,
                    )
                }
            }
        },
    ) { _ ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(bottom = PEEK)
                .safeDrawingPadding()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(Modifier.fillMaxWidth().heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    buildAnnotatedString {
                        val name = stringResource(R.string.app_name_full)
                        append(name)
                        // The kanji is set in Noto Serif JP; Newsreader has no glyph for it.
                        val kanji = name.indexOf('形')
                        if (kanji >= 0) addStyle(SpanStyle(fontFamily = LumeType.NotoSerifJP), kanji, kanji + 1)
                    },
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onSettings, Modifier.size(48.dp)) {
                    Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings))
                }
            }
            Column(
                Modifier.weight(1f).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                val number = day.nextNumber()
                Text(
                    if (number != null) stringResource(R.string.hero_puzzle, number, DailySet.TOTAL) else stringResource(R.string.hero_done_label),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Box(Modifier.padding(vertical = 16.dp).size(200.dp), contentAlignment = Alignment.Center) {
                    if (next != null) Preview(set, next.first, next.second) else Text(stringResource(R.string.hero_done), style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
                }
                if (next != null) {
                    Button(onClick = { onOpen(next.first, next.second) }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null)
                        Text(stringResource(R.string.hero_solve), Modifier.padding(start = 6.dp))
                    }
                } else {
                    Text(stringResource(R.string.home_done), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

private val PEEK = 170.dp

@Composable
private fun Chip(label: String, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.heightIn(min = 48.dp).pressable(onClick = onClick),
    ) {
        Box(Modifier.heightIn(min = 48.dp).padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun DrawerRow(title: String, value: String, onClick: (() -> Unit)?) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** What the next puzzle looks like, drawn from the day's set. */
@Composable
private fun Preview(set: DailySet, kind: Kind, index: Int) {
    when (kind) {
        Kind.TANGRAM -> Silhouette(set.tangrams[index].tangram)
        Kind.MATRIX -> GridView(set.matrices[index].cells[0], Modifier.fillMaxSize())
        Kind.BUILD -> GridView(set.builds[index].given, Modifier.fillMaxSize(), given = set.builds[index]::isGiven)
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
