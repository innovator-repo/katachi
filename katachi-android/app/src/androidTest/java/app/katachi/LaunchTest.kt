package app.katachi

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Opens the app on an emulator, checks the Lume core loaded, and opens one puzzle of each kind. */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class LaunchTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private fun back() = rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }

    @Test
    fun theLumeCoreLoads() {
        assertTrue("liblumecore.so did not load or failed its self-check", LumeCore.available)
    }

    @Test
    fun opensAPuzzleOfEachKind() {
        rule.waitUntilAtLeastOneExists(hasText("Tangram"), timeoutMillis = 30_000)

        rule.onAllNodesWithText("1")[0].performScrollTo().performClick()
        rule.onNodeWithText("Tangram 1 of 5").assertExists()
        rule.onNodeWithText("Flip").performClick()
        rule.onNodeWithText("Hint").performClick()
        back()

        rule.waitUntilAtLeastOneExists(hasText("Spot the pattern"), timeoutMillis = 10_000)
        rule.onAllNodesWithText("1")[1].performScrollTo().performClick()
        rule.onNodeWithText("Pattern 1 of 3").assertExists()
        back()

        rule.waitUntilAtLeastOneExists(hasText("Build the pattern"), timeoutMillis = 10_000)
        rule.onAllNodesWithText("1")[2].performScrollTo().performClick()
        rule.onNodeWithText("Build 1 of 2").assertExists()
        rule.onNodeWithText("Check").performClick()
        rule.waitUntilAtLeastOneExists(hasText("off. Keep going.", substring = true), timeoutMillis = 5_000)
        back()
    }
}
