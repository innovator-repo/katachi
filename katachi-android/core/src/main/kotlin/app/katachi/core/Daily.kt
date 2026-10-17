package app.katachi.core

import java.time.LocalDate
import kotlin.random.Random

enum class Kind { TANGRAM, MATRIX, BUILD }

class TangramPuzzle(val tangram: Tangram, val compact: Boolean, val outlinesShown: Int)

/**
 * The day's set. The date is the seed and the level is fixed for the day, so a
 * day always brings the same ten puzzles (reopening the app changes nothing)
 * and no two days are alike.
 */
class DailySet(val date: LocalDate, math: GridMath, val level: Int = 1) {
    val difficulty = Difficulty(level)

    val tangrams: List<TangramPuzzle> = List(TANGRAMS) { i ->
        val compact = difficulty.tangramCompact(i)
        TangramPuzzle(TangramMaker.make(random(Kind.TANGRAM, i), compact), compact, difficulty.tangramOutlines(i))
    }

    val matrices: List<MatrixPuzzle> = List(MATRICES) { i ->
        MatrixMaker.make(
            math, random(Kind.MATRIX, i), difficulty.matrixKinds[i],
            difficulty.matrixMoves, difficulty.matrixSizes, difficulty.subtleOptions,
        )
    }

    val builds: List<BuildPuzzle> = List(BUILDS) { i ->
        BuildMaker.make(math, random(Kind.BUILD, i), difficulty.buildRule(i, date.toEpochDay()), difficulty.buildFill)
    }

    private fun random(kind: Kind, index: Int) = Random(date.toEpochDay() * 1_000_003L + kind.ordinal * 101L + index)

    companion object {
        const val TANGRAMS = 5
        const val MATRICES = 3
        const val BUILDS = 2
        const val TOTAL = TANGRAMS + MATRICES + BUILDS

        fun id(kind: Kind, index: Int) = "${kind.name.first().lowercase()}$index"

        fun count(kind: Kind) = when (kind) {
            Kind.TANGRAM -> TANGRAMS
            Kind.MATRIX -> MATRICES
            Kind.BUILD -> BUILDS
        }

        /** Every puzzle of the day in play order: tangrams, then recognition, then building. */
        val order: List<Pair<Kind, Int>> = Kind.entries.flatMap { kind -> (0 until count(kind)).map { kind to it } }
    }
}
