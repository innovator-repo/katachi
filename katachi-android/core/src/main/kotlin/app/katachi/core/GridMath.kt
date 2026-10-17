package app.katachi.core

import kotlin.math.max

/** The operations `apply` in `lume/puzzles.lm` takes, by the same numbers. */
enum class GridOp(val code: Int) {
    TURN(0), MIRROR(1), FLIP(2), RIGHT(3), DOWN(4), INVERT(5),
    OVERLAY(6), DIFFER(7),
    MIRROR_FILL(8), QUARTER_FILL(9), HALF_TURN_FILL(10), TILE_FILL(11),
}

/**
 * The math Katachi runs on, with the exact shapes of `lume/puzzles.lm`.
 * A grid is [CELLS] row-major doubles, 0 or 1; a raster is [RASTER] x [RASTER].
 */
interface GridMath {
    /** [b] is only read by the two-grid operations; pass [EMPTY] otherwise. */
    fun apply(op: GridOp, a: DoubleArray, b: DoubleArray = EMPTY): DoubleArray

    /** How many cells two grids disagree on. */
    fun mismatch(a: DoubleArray, b: DoubleArray): Double

    /** The share of the silhouette's area that is uncovered, covered twice or spilled outside. */
    fun tangramError(target: DoubleArray, cover: DoubleArray): Double

    companion object {
        const val N = 6
        const val CELLS = N * N
        const val RASTER = 96

        /** Matches the 0.000001 the Lume source adds before dividing. */
        const val EPSILON = 0.000001

        val EMPTY = DoubleArray(CELLS)
    }
}

/** The same computations in Kotlin, used where the Lume library is not available and to check it. */
object KotlinMath : GridMath {
    private const val N = GridMath.N

    private inline fun grid(cell: (row: Int, col: Int) -> Double) =
        DoubleArray(GridMath.CELLS) { cell(it / N, it % N) }

    private fun DoubleArray.at(row: Int, col: Int) = this[Math.floorMod(row, N) * N + Math.floorMod(col, N)]

    private fun overlay(a: DoubleArray, b: DoubleArray) = DoubleArray(a.size) { a[it] + b[it] - a[it] * b[it] }

    private fun turn(x: DoubleArray) = grid { r, c -> x.at(N - 1 - c, r) }
    private fun mirror(x: DoubleArray) = grid { r, c -> x.at(r, N - 1 - c) }
    private fun flip(x: DoubleArray) = grid { r, c -> x.at(N - 1 - r, c) }
    private fun shiftBy(x: DoubleArray, rows: Int, cols: Int) = grid { r, c -> x.at(r - rows, c - cols) }

    override fun apply(op: GridOp, a: DoubleArray, b: DoubleArray): DoubleArray = when (op) {
        GridOp.TURN -> turn(a)
        GridOp.MIRROR -> mirror(a)
        GridOp.FLIP -> flip(a)
        GridOp.RIGHT -> shiftBy(a, 0, 1)
        GridOp.DOWN -> shiftBy(a, 1, 0)
        GridOp.INVERT -> DoubleArray(a.size) { 1.0 - a[it] }
        GridOp.OVERLAY -> overlay(a, b)
        GridOp.DIFFER -> DoubleArray(a.size) { (a[it] - b[it]) * (a[it] - b[it]) }
        GridOp.MIRROR_FILL -> overlay(a, mirror(a))
        GridOp.QUARTER_FILL -> overlay(a, mirror(a)).let { overlay(it, flip(it)) }
        GridOp.HALF_TURN_FILL -> overlay(a, turn(turn(a)))
        GridOp.TILE_FILL -> overlay(a, shiftBy(a, 0, 3)).let { overlay(it, shiftBy(it, 3, 0)) }
    }

    override fun mismatch(a: DoubleArray, b: DoubleArray): Double =
        a.indices.sumOf { (a[it] - b[it]) * (a[it] - b[it]) }

    override fun tangramError(target: DoubleArray, cover: DoubleArray): Double {
        var wrong = 0.0
        var area = 0.0
        for (i in target.indices) {
            wrong += max(0.0, target[i] - cover[i]) + max(0.0, cover[i] - target[i])
            area += target[i]
        }
        return wrong / (area + GridMath.EPSILON)
    }
}
