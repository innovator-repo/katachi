package app.katachi.designsystem

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import app.katachi.R

/** The four voices, shared with the other Lume apps. */
object LumeType {
    val Geist = FontFamily(
        Font(R.font.geist_regular, FontWeight.Normal),
        Font(R.font.geist_medium, FontWeight.Medium),
        Font(R.font.geist_semibold, FontWeight.SemiBold),
    )
    val GeistMono = FontFamily(Font(R.font.geist_mono_regular, FontWeight.Normal))
    val Newsreader = FontFamily(
        Font(R.font.newsreader_regular, FontWeight.Normal),
        Font(R.font.newsreader_semibold, FontWeight.SemiBold),
    )
    val NotoSerifJP = FontFamily(
        Font(R.font.noto_serif_jp_regular, FontWeight.Normal),
        Font(R.font.noto_serif_jp_semibold, FontWeight.SemiBold),
    )

    private fun style(size: Int, weight: FontWeight = FontWeight.Normal, family: FontFamily = Geist) =
        TextStyle(fontFamily = family, fontWeight = weight, fontSize = size.sp)

    val typography = Typography(
        headlineMedium = style(28, FontWeight.Medium, Newsreader),
        titleLarge = style(20, FontWeight.SemiBold),
        titleMedium = style(16, FontWeight.Medium),
        bodyLarge = style(16),
        bodyMedium = style(15),
        bodySmall = style(13),
        labelLarge = style(14, FontWeight.Medium),
        labelMedium = style(12, FontWeight.Medium),
        labelSmall = style(11, FontWeight.Medium, GeistMono),
    )
}
