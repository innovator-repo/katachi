package app.katachi

import app.katachi.designsystem.HapticRecipes
import app.katachi.designsystem.HapticToken
import org.junit.Assert.assertEquals
import org.junit.Test

class HapticRecipesTest {
    @Test fun tickIsOneHalfStrengthTick() {
        assertEquals(listOf(HapticRecipes.TICK to 0.5f), HapticRecipes.of(HapticToken.TICK).map { it.primitive to it.scale })
    }

    @Test fun gentleNudgeIsTwoLowTicks() {
        val steps = HapticRecipes.of(HapticToken.GENTLE_NUDGE)
        assertEquals(listOf(HapticRecipes.LOW_TICK, HapticRecipes.LOW_TICK), steps.map { it.primitive })
        assertEquals(100, steps[1].delayMs)
    }

    @Test fun everyTokenHasARecipeAndAFallbackOfMatchingLength() {
        for (t in HapticToken.entries) {
            val (timings, amplitudes) = HapticRecipes.fallback(t)
            assertEquals(HapticRecipes.of(t).size * 2, timings.size)
            assertEquals(timings.size, amplitudes.size)
        }
    }
}
