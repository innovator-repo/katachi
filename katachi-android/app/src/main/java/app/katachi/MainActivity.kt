package app.katachi

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import app.katachi.core.DailySet
import app.katachi.core.Kind
import app.katachi.designsystem.LumeSpinner
import app.katachi.ui.BuildScreen
import app.katachi.ui.HomeScreen
import app.katachi.ui.KatachiTheme
import app.katachi.ui.MatrixScreen
import app.katachi.ui.SettingsScreen
import app.katachi.ui.StatsScreen
import app.katachi.ui.StreakScreen
import app.katachi.ui.TangramScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate

class MainActivity : ComponentActivity() {
    // Checked on every resume, so a set left open overnight turns into the new day's.
    private val today = mutableStateOf(LocalDate.now())

    // Set by katachi://puzzle/next; the screen opens the next unsolved puzzle and clears it.
    private val wantsNext = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) noteDeepLink(intent)
        setContent { KatachiTheme { Katachi(today, wantsNext) } }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        noteDeepLink(intent)
    }

    private fun noteDeepLink(intent: Intent?) {
        val uri = intent?.data ?: return
        if (uri.scheme == "katachi" && uri.host == "puzzle" && uri.path == "/next") wantsNext.value = true
    }

    override fun onResume() {
        super.onResume()
        if (today.value != LocalDate.now()) today.value = LocalDate.now()
    }
}

/** The puzzle on screen, or null for the day's list. Kind and index, so it survives rotation. */
private data class Open(val kind: Kind, val index: Int)

/** The order "Next" walks through: tangrams, then recognition, then building. */
private val order = DailySet.order.map { (kind, i) -> Open(kind, i) }

@Composable
private fun Katachi(todayState: MutableState<LocalDate>, wantsNext: MutableState<Boolean>) {
    val today by todayState
    val store = LocalContext.current.store
    // Earlier days only, so the level holds still while today's puzzles are being solved.
    val level = remember(today) { store.history.value.level(today) }
    val set by produceState<DailySet?>(null, today, level) {
        value = null
        value = withContext(Dispatchers.Default) { DailySet(today, LumeCore.active, level) }
    }
    var openKind by rememberSaveable(today) { mutableStateOf<Kind?>(null) }
    var openIndex by rememberSaveable(today) { mutableStateOf(0) }
    // The screen behind a chip or the gear: "stats", "streak" or "settings"; null is the main screen.
    var sub by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(set, wantsNext.value) {
        if (set == null || !wantsNext.value) return@LaunchedEffect
        wantsNext.value = false
        val solved = store.history.value.day(today)
        order.firstOrNull { !solved.solved(DailySet.id(it.kind, it.index)) }?.let { openKind = it.kind; openIndex = it.index }
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        val daily = set
        if (daily == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { LumeSpinner() }
            return@Surface
        }
        val shown = openKind?.let { Open(it, openIndex) }
        if (shown != null) BackHandler { openKind = null } else if (sub != null) BackHandler { sub = null }
        AnimatedContent(
            shown,
            transitionSpec = {
                // Into a puzzle slides in from the right; back home slides the other way.
                val forward = targetState != null && (initialState == null || order.indexOf(targetState!!) > order.indexOf(initialState!!))
                val sign = if (forward) 1 else -1
                (slideInHorizontally(app.katachi.designsystem.LumeMotion.spatial()) { it * sign / 3 } + fadeIn(app.katachi.designsystem.LumeMotion.effects())) togetherWith
                    (slideOutHorizontally(app.katachi.designsystem.LumeMotion.spatial()) { -it * sign / 4 } + fadeOut(app.katachi.designsystem.LumeMotion.effects()))
            },
            label = "screen",
        ) { open ->
            if (open == null) {
                when (sub) {
                    "stats" -> StatsScreen(today) { sub = null }
                    "streak" -> StreakScreen(today) { sub = null }
                    "settings" -> SettingsScreen(today) { sub = null }
                    else -> HomeScreen(
                        daily,
                        onOpen = { k, i -> openKind = k; openIndex = i },
                        onStats = { sub = "stats" },
                        onStreak = { sub = "streak" },
                        onSettings = { sub = "settings" },
                    )
                }
                return@AnimatedContent
            }
            val close = { openKind = null }
            val next = {
                val solved = store.history.value.day(today)
                val here = order.indexOf(open)
                val upcoming = (order.drop(here + 1) + order.take(here + 1))
                    .firstOrNull { !solved.solved(DailySet.id(it.kind, it.index)) }
                if (upcoming == null) openKind = null else { openKind = upcoming.kind; openIndex = upcoming.index }
            }
            when (open.kind) {
                Kind.TANGRAM -> TangramScreen(daily, open.index, onBack = close, onNext = next)
                Kind.MATRIX -> MatrixScreen(daily, open.index, onBack = close, onNext = next)
                Kind.BUILD -> BuildScreen(daily, open.index, onBack = close, onNext = next)
            }
        }
    }
}
