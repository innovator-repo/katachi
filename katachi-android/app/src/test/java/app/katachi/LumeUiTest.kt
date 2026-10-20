package app.katachi

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import app.katachi.core.DailySet
import app.katachi.core.KotlinMath
import app.katachi.core.Phase
import app.katachi.designsystem.LumeSemantic
import app.katachi.designsystem.LumeType
import app.katachi.designsystem.lumeColorScheme
import app.katachi.designsystem.surfacesFor
import app.katachi.ui.BuildScreen
import app.katachi.ui.HomeScreen
import app.katachi.ui.MatrixScreen
import app.katachi.ui.TangramScreen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(application = KatachiApp::class, sdk = [34])
class LumeUiTest {
    @get:Rule val rule = createComposeRule()

    private val app get() = ApplicationProvider.getApplicationContext<KatachiApp>()
    private val set = DailySet(LocalDate.of(2026, 3, 4), KotlinMath, 1)

    private fun themed(phase: Phase, dark: Boolean, content: @Composable () -> Unit) = rule.setContent {
        MaterialTheme(colorScheme = lumeColorScheme(surfacesFor(phase, dark)), typography = LumeType.typography) { content() }
    }

    @Test fun everyScreenRendersInEveryPhaseAndMode() {
        var phase by mutableStateOf(Phase.DAY)
        var dark by mutableStateOf(false)
        var screen by mutableIntStateOf(0)
        rule.setContent {
            MaterialTheme(colorScheme = lumeColorScheme(surfacesFor(phase, dark)), typography = LumeType.typography) {
                key(phase, dark, screen) {
                    when (screen) {
                        0 -> HomeScreen(set) { _, _ -> }
                        1 -> TangramScreen(set, 0, {}, {})
                        2 -> MatrixScreen(set, 0, {}, {})
                        else -> BuildScreen(set, 0, {}, {})
                    }
                }
            }
        }
        for (p in Phase.entries) for (d in listOf(false, true)) for (sc in 0..3) {
            rule.runOnUiThread { phase = p; dark = d; screen = sc }
            rule.waitForIdle()
        }
    }

    @Test fun noPhaseUsesDestructiveRedForMisses() {
        for (phase in Phase.entries) for (dark in listOf(false, true)) {
            val scheme = lumeColorScheme(surfacesFor(phase, dark))
            assertEquals(LumeSemantic.Terracotta, scheme.error)
            assertNotEquals(LumeSemantic.Destructive, scheme.error)
            assertNotEquals(Color(0xFFF2B8B5), scheme.error)
        }
    }

    @Test fun wrongMatrixPickShowsACalmMessageAndKeepsGoing() {
        themed(Phase.DAY, false) { MatrixScreen(set, 0, {}, {}) }
        val wrong = (0 until set.matrices[0].options.size).first { it != set.matrices[0].answer }
        rule.onNodeWithTag("option$wrong").performClick()
        rule.waitForIdle()
        rule.onNodeWithText(app.getString(R.string.matrix_wrong)).assertExists()
        // The right answer is still available and still solves it.
        rule.onNodeWithTag("option${set.matrices[0].answer}").performClick()
        rule.waitForIdle()
        assertTrue(app.store.history.value.day(set.date).results.isNotEmpty())
    }
}
