package app.katachi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import app.katachi.core.DailySet
import app.katachi.core.Kind
import app.katachi.ui.BuildScreen
import app.katachi.ui.HomeScreen
import app.katachi.ui.KatachiTheme
import app.katachi.ui.MatrixScreen
import app.katachi.ui.TangramScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate

class MainActivity : ComponentActivity() {
    // Checked on every resume, so a set left open overnight turns into the new day's.
    private val today = mutableStateOf(LocalDate.now())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { KatachiTheme { Katachi(today) } }
    }

    override fun onResume() {
        super.onResume()
        if (today.value != LocalDate.now()) today.value = LocalDate.now()
    }
}

/** The puzzle on screen, or null for the day's list. Kind and index, so it survives rotation. */
private data class Open(val kind: Kind, val index: Int)

/** The order "Next" walks through: tangrams, then recognition, then building. */
private val order = Kind.entries.flatMap { kind ->
    val count = when (kind) {
        Kind.TANGRAM -> DailySet.TANGRAMS
        Kind.MATRIX -> DailySet.MATRICES
        Kind.BUILD -> DailySet.BUILDS
    }
    (0 until count).map { Open(kind, it) }
}

@Composable
private fun Katachi(todayState: MutableState<LocalDate>) {
    val today by todayState
    val set by produceState<DailySet?>(null, today) {
        value = null
        value = withContext(Dispatchers.Default) { DailySet(today, LumeCore.active) }
    }
    var openKind by rememberSaveable(today) { mutableStateOf<Kind?>(null) }
    var openIndex by rememberSaveable(today) { mutableStateOf(0) }
    val store = LocalContext.current.store

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        val daily = set
        if (daily == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Surface
        }
        val kind = openKind
        if (kind == null) {
            HomeScreen(daily, onOpen = { k, i -> openKind = k; openIndex = i })
            return@Surface
        }
        BackHandler { openKind = null }
        val close = { openKind = null }
        val next = {
            val solved = store.history.value.day(today)
            val here = order.indexOf(Open(kind, openIndex))
            val upcoming = (order.drop(here + 1) + order.take(here + 1))
                .firstOrNull { !solved.solved(DailySet.id(it.kind, it.index)) }
            if (upcoming == null) openKind = null else { openKind = upcoming.kind; openIndex = upcoming.index }
        }
        when (kind) {
            Kind.TANGRAM -> TangramScreen(daily, openIndex, onBack = close, onNext = next)
            Kind.MATRIX -> MatrixScreen(daily, openIndex, onBack = close, onNext = next)
            Kind.BUILD -> BuildScreen(daily, openIndex, onBack = close, onNext = next)
        }
    }
}
