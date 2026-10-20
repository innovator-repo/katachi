package app.katachi.designsystem

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * A top or bottom bar that lets the content show faintly through on Android 12+, and is
 * fully opaque below. (The other apps frost it with Haze; Katachi has no scrolling
 * content under its bars yet, so the translucent fill is all it needs.)
 */
@Composable
fun LumeBar(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val alpha = if (Build.VERSION.SDK_INT >= 31) 0.88f else 1f
    Box(modifier.background(MaterialTheme.colorScheme.background.copy(alpha = alpha))) { content() }
}
