package app.katachi

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
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

    /** Pulls the Today drawer up (a swipe on its title; harmless if it is already open). */
    private fun openDrawer() {
        rule.waitUntilAtLeastOneExists(hasText("Today"), timeoutMillis = 30_000)
        rule.onNodeWithText("Today").performTouchInput { swipeUp() }
        rule.waitForIdle()
    }

    @Test
    fun opensAPuzzleOfEachKind() {
        // The hero's Solve button opens the next unsolved puzzle: the first tangram on a fresh install.
        rule.waitUntilAtLeastOneExists(hasText("Solve it"), timeoutMillis = 30_000)
        rule.onNodeWithText("Solve it").performClick()
        rule.onNodeWithText("Tangram 1 of 5").assertExists()
        rule.onNodeWithText("Flip").performClick()
        rule.onNodeWithText("Hint").performClick()
        back()

        openDrawer()
        rule.onNodeWithText("Spot the pattern").performClick()
        rule.onNodeWithText("Pattern 1 of 3").assertExists()
        back()

        openDrawer()
        rule.onNodeWithText("Build the pattern").performClick()
        rule.onNodeWithText("Build 1 of 2").assertExists()
        rule.onNodeWithText("Check").performClick()
        rule.waitUntilAtLeastOneExists(hasText("off. Keep going.", substring = true), timeoutMillis = 5_000)
        back()
    }
}
