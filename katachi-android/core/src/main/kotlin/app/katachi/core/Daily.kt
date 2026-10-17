package app.katachi.core

import java.time.LocalDate
import kotlin.random.Random

enum class Kind { TANGRAM, MATRIX, BUILD }

class TangramPuzzle(val tangram: Tangram, val compact: Boolean, val outlinesShown: Int)

/**
 * The day's set. The date is the seed, so a day always brings the same ten
 * puzzles (reopening the app changes nothing) and no two days are alike.
 */
class DailySet(val date: LocalDate, math: GridMath) {
    val tangrams: List<TangramPuzzle> = List(TANGRAMS) { i ->
        // Difficulty climbs: piece outlines shown 2, 1, 0, 0, 0, and the last two are compact figures.
        val compact = i >= 3
        TangramPuzzle(TangramMaker.make(random(Kind.TANGRAM, i), compact), compact, outlinesShown = maxOf(0, 2 - i))
    }

    val matrices: List<MatrixPuzzle> = List(MATRICES) { i -> MatrixMaker.make(math, random(Kind.MATRIX, i), level = i) }

    val builds: List<BuildPuzzle> = List(BUILDS) { i ->
        val day = date.toEpochDay().toInt()
        val rule = if (i == 0) listOf(BuildRule.MIRROR, BuildRule.HALF_TURN)[Math.floorMod(day, 2)]
        else listOf(BuildRule.QUARTER, BuildRule.TILE)[Math.floorMod(day / 2, 2)]
        BuildMaker.make(math, random(Kind.BUILD, i), rule)
    }

    private fun random(kind: Kind, index: Int) = Random(date.toEpochDay() * 1_000_003L + kind.ordinal * 101L + index)

    companion object {
        const val TANGRAMS = 5
        const val MATRICES = 3
        const val BUILDS = 2
        const val TOTAL = TANGRAMS + MATRICES + BUILDS

        fun id(kind: Kind, index: Int) = "${kind.name.first().lowercase()}$index"
    }
}
