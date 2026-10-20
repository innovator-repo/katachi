package app.katachi.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import app.katachi.designsystem.LocalPhase
import app.katachi.designsystem.LumeType
import app.katachi.designsystem.lumeColorScheme
import app.katachi.designsystem.rememberPhase
import app.katachi.designsystem.surfacesFor
import androidx.compose.foundation.isSystemInDarkTheme

@Composable
fun KatachiTheme(content: @Composable () -> Unit) {
    val phase = rememberPhase()
    val colors = lumeColorScheme(surfacesFor(phase, isSystemInDarkTheme()))
    CompositionLocalProvider(LocalPhase provides phase) {
        MaterialTheme(colorScheme = colors, typography = LumeType.typography, content = content)
    }
}
