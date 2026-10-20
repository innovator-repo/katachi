package app.katachi.core

import kotlin.math.abs
import kotlin.math.atan2

/** How many puzzles count as "done today": five normally, one on a floor-mode (low-energy) day. */
object DailyTarget {
    const val NORMAL = 5
    const val FLOOR = 1

    fun of(floorMode: Boolean) = if (floorMode) FLOOR else NORMAL
}

/** The four circadian surface phases. Evening and sleep always use dark surfaces. */
enum class Phase {
    MORNING, DAY, EVENING, SLEEP;

    companion object {
        /** 06:00-11:59 morning, 12:00-19:59 day, 20:00-21:59 evening, 22:00-05:59 sleep. */
        fun at(hour: Int): Phase = when (hour) {
            in 6..11 -> MORNING
            in 12..19 -> DAY
            in 20..21 -> EVENING
            else -> SLEEP
        }
    }
}

/** The JSON Kasane stores for Analysis; built by hand so the core needs no extra dependency. */
object EcoMetrics {
    fun payload(solved: Int, target: Int, solveTimeMs: Long) =
        """{"solved":$solved,"target":$target,"solveTimeMs":$solveTimeMs}"""
}

/** Magnetic attraction between a dragged piece and the corners it could snap to. */
object Magnet {
    /** How far a dragged piece may be turned off an edge it lines up with, in degrees. */
    const val MAX_DEGREES = 15.0

    /**
     * The move that would put one of [piece]'s corners on a corner of [others], if one is within
     * [reach] tangram units and some edge of the piece runs within [MAX_DEGREES] of an edge of
     * the polygon that corner belongs to. Null when nothing attracts the piece.
     */
    fun pull(piece: List<Vec>, others: List<List<Vec>>, reach: Double): Vec? {
        var best: Vec? = null
        var bestDistance = reach
        for (v in piece) for (poly in others) for (a in poly) {
            val d = (a - v).length
            if (d <= bestDistance && edgesAlign(piece, poly)) {
                bestDistance = d
                best = a - v
            }
        }
        return best
    }

    private fun edgesAlign(a: List<Vec>, b: List<Vec>): Boolean {
        for (i in a.indices) {
            val ea = a[(i + 1) % a.size] - a[i]
            for (j in b.indices) {
                val eb = b[(j + 1) % b.size] - b[j]
                if (degreesBetween(ea, eb) <= MAX_DEGREES) return true
            }
        }
        return false
    }

    /** Smallest angle between two lines (direction ignored), 0..90. */
    fun degreesBetween(a: Vec, b: Vec): Double {
        val turn = Math.toDegrees(abs(atan2(a.cross(b), a.dot(b))))
        return minOf(turn, 180.0 - turn)
    }
}
