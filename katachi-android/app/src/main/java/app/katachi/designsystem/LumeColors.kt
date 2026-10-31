package app.katachi.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Color
import app.katachi.core.Phase
import kotlinx.coroutines.delay
import java.time.LocalTime

/** Katachi's accent: carved imperial jade. */
object LumeAccent {
    val Jade = Color(0xFF1E7D68)
    val JadeLight = Color(0xFF5DD2B0)
}

/** Tokens shared across the apps: calm semantic colours, and the seal colour. */
object LumeSemantic {
    /** A miss or a slip. Warm and calm, never alarming. */
    val Terracotta = Color(0xFFC95B3C)

    /** Reserved for destructive deletion; nothing in Katachi uses it. */
    val Destructive = Color(0xFFB3261E)
    val Seal = Color(0xFF5856D6)
}

/** The seven tangram pieces: natural mineral and wood tints, in the order of the piece set. */
object TangramPalette {
    val pieces = listOf(
        Color(0xFFE06D53), // terracotta
        Color(0xFFD99B52), // hinoki cypress
        Color(0xFF3F9E66), // bamboo
        Color(0xFF2E9696), // celadon
        Color(0xFF4A6FE3), // indigo
        Color(0xFF7E57C2), // plum
        Color(0xFF6B7A8F), // slate
    )
}

/** Surfaces for one phase of the day. */
data class Surfaces(val canvas: Color, val grouped: Color, val ink: Color, val inkSoft: Color, val line: Color, val dark: Boolean)

private val WashiLight = Surfaces(Color(0xFFFAF8F5), Color(0xFFF1EEE8), Color(0xFF1A1B1A), Color(0xFF5E605C), Color(0xFFDDD9D0), false)
private val WashiDark = Surfaces(Color(0xFF141716), Color(0xFF1F2321), Color(0xFFE9ECEA), Color(0xFFA2A9A5), Color(0xFF2E3431), true)
private val Graphite = Surfaces(Color(0xFF181715), Color(0xFF232120), Color(0xFFEAE6DF), Color(0xFFA9A49A), Color(0xFF34312D), true)
private val Obsidian = Surfaces(Color(0xFF0B0F14), Color(0xFF141A21), Color(0xFFC9D0D8), Color(0xFF87909B), Color(0xFF222A33), true)

/**
 * Circadian surfaces. Morning and day follow the phone's light/dark setting on the warm washi
 * canvas (Katachi keeps washi by day too: pure white glares behind dragged pieces). Evening is
 * warm graphite and sleep is obsidian, whatever the setting.
 */
fun surfacesFor(phase: Phase, systemDark: Boolean): Surfaces = when (phase) {
    Phase.MORNING, Phase.DAY -> if (systemDark) WashiDark else WashiLight
    Phase.EVENING -> Graphite
    Phase.SLEEP -> Obsidian
}

fun lumeColorScheme(s: Surfaces): ColorScheme {
    val accent = if (s.dark) LumeAccent.JadeLight else LumeAccent.Jade
    return if (s.dark) {
        darkColorScheme(
            primary = accent, onPrimary = Color(0xFF06372B),
            primaryContainer = Color(0xFF1B4D41), onPrimaryContainer = Color(0xFFD9F2EA),
            background = s.canvas, onBackground = s.ink, surface = s.canvas, onSurface = s.ink,
            surfaceVariant = s.grouped, onSurfaceVariant = s.inkSoft, outline = s.line,
            error = LumeSemantic.Terracotta,
        )
    } else {
        lightColorScheme(
            primary = accent, onPrimary = Color.White,
            primaryContainer = Color(0xFFD9F2EA), onPrimaryContainer = Color(0xFF0B3A2F),
            background = s.canvas, onBackground = s.ink, surface = s.canvas, onSurface = s.ink,
            surfaceVariant = s.grouped, onSurfaceVariant = s.inkSoft, outline = s.line,
            error = LumeSemantic.Terracotta,
        )
    }
}

val LocalPhase = compositionLocalOf { Phase.DAY }

/** The current phase, re-read every minute so a screen left open drifts into the evening by itself. */
@Composable
fun rememberPhase(): Phase {
    var phase by remember { mutableStateOf(Phase.at(LocalTime.now().hour)) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000)
            phase = Phase.at(LocalTime.now().hour)
        }
    }
    return phase
}

@Composable
fun rememberSurfaces(): Surfaces = surfacesFor(rememberPhase(), isSystemInDarkTheme())
