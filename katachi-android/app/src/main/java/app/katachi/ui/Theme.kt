package app.katachi.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import app.katachi.R

/** Geist, the same family as Ren, so the apps read as one set. */
val Geist = FontFamily(
    Font(R.font.geist_regular, FontWeight.Normal),
    Font(R.font.geist_medium, FontWeight.Medium),
    Font(R.font.geist_semibold, FontWeight.SemiBold),
)

/** Jade, the colour of a carved shape. */
private val Jade = Color(0xFF1E7D68)
private val JadeLight = Color(0xFF8FD9C3)

internal val Light = lightColorScheme(
    primary = Jade,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD9F2EA),
    onPrimaryContainer = Color(0xFF0B3A2F),
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF15161A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF15161A),
    surfaceVariant = Color(0xFFF2F3F6),
    onSurfaceVariant = Color(0xFF5B5F6B),
    outline = Color(0xFFD9DBE1),
    error = Color(0xFFB3261E),
)

internal val Dark = darkColorScheme(
    primary = JadeLight,
    onPrimary = Color(0xFF06372B),
    primaryContainer = Color(0xFF1B4D41),
    onPrimaryContainer = Color(0xFFD9F2EA),
    background = Color(0xFF0F1013),
    onBackground = Color(0xFFE8E9ED),
    surface = Color(0xFF0F1013),
    onSurface = Color(0xFFE8E9ED),
    surfaceVariant = Color(0xFF1C1E23),
    onSurfaceVariant = Color(0xFFA3A7B3),
    outline = Color(0xFF2E3138),
    error = Color(0xFFF2B8B5),
)

private fun style(size: Int, weight: FontWeight = FontWeight.Normal) =
    TextStyle(fontFamily = Geist, fontWeight = weight, fontSize = size.sp)

private val Type = Typography(
    headlineMedium = style(28, FontWeight.SemiBold),
    titleLarge = style(20, FontWeight.SemiBold),
    titleMedium = style(16, FontWeight.Medium),
    bodyLarge = style(16),
    bodyMedium = style(15),
    bodySmall = style(13),
    labelLarge = style(14, FontWeight.Medium),
    labelMedium = style(12, FontWeight.Medium),
    labelSmall = style(11, FontWeight.Medium),
)

@Composable
fun KatachiTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) Dark else Light, typography = Type, content = content)
}
