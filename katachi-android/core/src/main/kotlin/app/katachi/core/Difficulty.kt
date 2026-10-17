package app.katachi.core

import kotlin.math.max
import kotlin.math.min

/**
 * What a level means for each kind of puzzle. Level 1 is gentle; every knob
 * turns a notch every few levels, so no single day is a jump. Within a day
 * the set still climbs from its first puzzle to its last.
 */
data class Difficulty(val level: Int) {
    private val step = level - 1

    /** Piece outlines drawn on tangram [index]: level 1 starts at 3, 2, 1, 0, 0; from level 10 there are none. */
    fun tangramOutlines(index: Int) = max(0, 3 - step / 3 - index)

    /** The last few tangrams of the day are compact figures, the hard kind: one at level 1, all five by level 17. */
    fun tangramCompact(index: Int) = index >= DailySet.TANGRAMS - min(DailySet.TANGRAMS, 1 + step / 4)

    /** Which rule family each of the day's three matrices uses (see [MatrixMaker.make]). */
    val matrixKinds: List<Int> = when {
        level <= 2 -> listOf(0, 0, 0)
        level <= 5 -> listOf(0, 0, 1)
        level <= 8 -> listOf(0, 1, 1)
        level <= 11 -> listOf(0, 1, 2)
        else -> listOf(1, 2, 2)
    }

    /** Shifts and a mirror first; the flip at level 3, the quarter turn, hardest to see, at level 5. */
    val matrixMoves: List<GridOp> = buildList {
        addAll(listOf(GridOp.RIGHT, GridOp.DOWN, GridOp.MIRROR))
        if (level >= 3) add(GridOp.FLIP)
        if (level >= 5) add(GridOp.TURN)
    }

    /** Shapes grow from 4–6 squares to 7–9. */
    val matrixSizes: IntRange = (4 + min(3, step / 4)).let { it..it + 2 }

    /** From level 6 the wrong options include near-misses one square off. */
    val subtleOptions = level >= 6

    /** Mirror and half turn first; the four-way and tile rules join at level 4, and any rule can come up from level 8. */
    fun buildRule(index: Int, day: Long): BuildRule {
        val easy = listOf(BuildRule.MIRROR, BuildRule.HALF_TURN)
        val hard = listOf(BuildRule.QUARTER, BuildRule.TILE)
        val pool = when {
            level < 4 -> easy
            level < 8 -> if (index == 0) easy else hard
            else -> if (index == 0) easy + hard else hard
        }
        return pool[Math.floorMod(day + index, pool.size.toLong()).toInt()]
    }

    /** More of the given part filled in means more to carry across: 35% at level 1, up to 55%. */
    val buildFill = 0.35 + 0.02 * min(10, step)

    companion object {
        const val MAX = 30

        /** Solving this many of a day's ten puzzles raises the level by one for the next day. */
        const val TO_LEVEL_UP = 8
    }
}
