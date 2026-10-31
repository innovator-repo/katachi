package app.katachi.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.katachi.R
import app.katachi.core.DailySet
import app.katachi.core.Kind
import app.katachi.core.bestSeconds
import app.katachi.core.bestStreak
import app.katachi.core.dailyCounts
import app.katachi.core.totalSolved
import app.katachi.designsystem.GroupedList
import app.katachi.designsystem.GroupedRow
import app.katachi.lumeState
import app.katachi.store
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/** A secondary screen's frame: a back arrow labelled with the app name, a title, then the content. */
@Composable
private fun SubScreen(title: String, onBack: () -> Unit, content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(Modifier.fillMaxWidth().heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, Modifier.size(48.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
            }
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleMedium)
        }
        Text(title, style = MaterialTheme.typography.headlineMedium)
        content()
    }
}

/** Solved total, the best time for each kind, and the last 14 days as bars. */
@Composable
fun StatsScreen(today: LocalDate, onBack: () -> Unit) {
    val history by LocalContext.current.store.history.collectAsState()
    val counts = history.dailyCounts(today, 14)
    val none = stringResource(R.string.none_yet)
    SubScreen(stringResource(R.string.chip_stats), onBack) {
        GroupedList {
            GroupedRow(stringResource(R.string.stats_total), history.totalSolved().toString(), first = true)
            for (kind in Kind.entries) {
                GroupedRow(
                    stringResource(R.string.stats_best, stringResource(kindTitleRes(kind))),
                    history.bestSeconds(kind)?.let { formatSeconds(it) } ?: none,
                )
            }
        }
        Text(stringResource(R.string.stats_last14), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
        if (counts.all { it == 0 }) {
            Text(stringResource(R.string.stats_from_today), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Bars(counts)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            val labels = listOf(today.minusDays(13), today)
            for (d in labels) Text(
                d.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()) + " " + d.dayOfMonth,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun Bars(counts: List<Int>) {
    val fill = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.surfaceVariant
    val top = DailySet.TOTAL.toFloat()
    Canvas(Modifier.fillMaxWidth().height(140.dp)) {
        val gap = 6.dp.toPx()
        val w = (size.width - gap * (counts.size - 1)) / counts.size
        val radius = CornerRadius(w / 4)
        counts.forEachIndexed { i, n ->
            val x = i * (w + gap)
            drawRoundRect(track, Offset(x, 0f), Size(w, size.height), radius)
            if (n > 0) {
                val h = size.height * (n / top).coerceAtMost(1f)
                drawRoundRect(fill, Offset(x, size.height - h), Size(w, h), radius)
            }
        }
    }
}

/** The current run of whole-set days and the best run. Both come from the stored history. */
@Composable
fun StreakScreen(today: LocalDate, onBack: () -> Unit) {
    val history by LocalContext.current.store.history.collectAsState()
    val current = history.streak(today)
    val best = history.bestStreak()
    SubScreen(stringResource(R.string.chip_streak), onBack) {
        Text(
            pluralStringResource(R.plurals.home_streak, current, current),
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(stringResource(R.string.row_streak), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        GroupedList {
            GroupedRow(stringResource(R.string.streak_current), pluralStringResource(R.plurals.home_streak, current, current), first = true)
            GroupedRow(stringResource(R.string.streak_best), pluralStringResource(R.plurals.home_streak, best, best))
        }
        if (best == 0) {
            Text(stringResource(R.string.streak_from_today), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** The few settings Katachi has: the low-energy-day switch. */
@Composable
fun SettingsScreen(today: LocalDate, onBack: () -> Unit) {
    val state = LocalContext.current.lumeState
    val floorDate by state.floorOn.collectAsState()
    val floor = floorDate == today.toString()
    val context = LocalContext.current
    val version = remember { runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "" }
    SubScreen(stringResource(R.string.settings), onBack) {
        GroupedList {
            Row(
                Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
                    Text(stringResource(R.string.settings_floor), style = MaterialTheme.typography.bodyLarge)
                    Text(stringResource(R.string.home_floor), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = floor, onCheckedChange = { state.setFloor(it, today) })
            }
            GroupedRow(stringResource(R.string.settings_version), version)
        }
    }
}

fun kindTitleRes(kind: Kind) = when (kind) {
    Kind.TANGRAM -> R.string.section_tangram
    Kind.MATRIX -> R.string.section_matrix
    Kind.BUILD -> R.string.section_build
}
