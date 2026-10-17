package app.katachi.core

import kotlin.random.Random

private const val N = GridMath.N

/** A random connected shape of [size] cells inside rows [rows] and columns [cols]. */
internal fun blob(random: Random, size: Int, rows: IntRange = 0 until N, cols: IntRange = 0 until N): DoubleArray {
    val grid = DoubleArray(GridMath.CELLS)
    val cells = mutableListOf(rows.random(random) to cols.random(random))
    grid[cells[0].first * N + cells[0].second] = 1.0
    var guard = 0
    while (cells.size < size && guard++ < 500) {
        val (r, c) = cells.random(random)
        val (dr, dc) = listOf(0 to 1, 1 to 0, 0 to -1, -1 to 0).random(random)
        val nr = r + dr
        val nc = c + dc
        if (nr in rows && nc in cols && grid[nr * N + nc] == 0.0) {
            grid[nr * N + nc] = 1.0
            cells += nr to nc
        }
    }
    return grid
}

/**
 * Pattern recognition: a 3 × 3 matrix of grids following a rule, the last
 * one hidden, and four options of which [answer] is the right one.
 */
class MatrixPuzzle(
    val cells: List<DoubleArray>,
    val options: List<DoubleArray>,
    val answer: Int,
    val rule: String,
)

object MatrixMaker {
    private val allMoves = listOf(GridOp.TURN, GridOp.MIRROR, GridOp.FLIP, GridOp.RIGHT, GridOp.DOWN)

    private fun describe(op: GridOp) = when (op) {
        GridOp.TURN -> "turns a quarter turn clockwise"
        GridOp.MIRROR -> "is mirrored left to right"
        GridOp.FLIP -> "is flipped top to bottom"
        GridOp.RIGHT -> "moves one square right, wrapping round"
        GridOp.DOWN -> "moves one square down, wrapping round"
        else -> op.name.lowercase()
    }

    /**
     * kind 0: one move along each row. 1: the third grid combines the first two. 2: a move along rows and
     * another down columns. [moves] are the moves this level may use, [sizes] how many squares a shape has,
     * and [subtle] prefers wrong options that differ from the answer by a single square.
     */
    fun make(
        math: GridMath,
        random: Random,
        kind: Int,
        moves: List<GridOp> = allMoves,
        sizes: IntRange = 5..7,
        subtle: Boolean = false,
    ): MatrixPuzzle {
        while (true) attempt(math, random, kind, moves, sizes, subtle)?.let { return it }
    }

    private fun attempt(
        math: GridMath,
        random: Random,
        level: Int,
        moves: List<GridOp>,
        sizes: IntRange,
        subtle: Boolean,
    ): MatrixPuzzle? {
        val cells = mutableListOf<DoubleArray>()
        val rule: String
        when (level) {
            0 -> {
                val move = moves.random(random)
                repeat(3) {
                    val a = blob(random, sizes.random(random))
                    val b = math.apply(move, a)
                    val c = math.apply(move, b)
                    if (math.mismatch(a, b) == 0.0) return null
                    cells += listOf(a, b, c)
                }
                rule = "Along each row the shape ${describe(move)}."
            }
            1 -> {
                val op = listOf(GridOp.OVERLAY, GridOp.DIFFER).random(random)
                repeat(3) {
                    val a = blob(random, sizes.random(random))
                    val b = blob(random, sizes.random(random))
                    // The two shapes must overlap, or "both" and "either but not both" look the same.
                    if (math.mismatch(math.apply(GridOp.OVERLAY, a, b), math.apply(GridOp.DIFFER, a, b)) == 0.0) return null
                    cells += listOf(a, b, math.apply(op, a, b))
                }
                rule = if (op == GridOp.OVERLAY) "The third grid is the first two laid on top of each other."
                else "The third grid keeps the squares in exactly one of the first two; where they overlap, they cancel."
            }
            else -> {
                val across = moves.random(random)
                val downward = (moves - across).random(random)
                val start = blob(random, sizes.random(random))
                val a = math.apply(across, math.apply(downward, start))
                val b = math.apply(downward, math.apply(across, start))
                if (math.mismatch(a, b) != 0.0) return null
                var rowStart = start
                repeat(3) {
                    val first = rowStart
                    val second = math.apply(across, first)
                    if (math.mismatch(first, second) == 0.0 || math.mismatch(first, math.apply(downward, first)) == 0.0) return null
                    cells += listOf(first, second, math.apply(across, second))
                    rowStart = math.apply(downward, rowStart)
                }
                rule = "Along each row the shape ${describe(across)}; down each column it ${describe(downward)}."
            }
        }
        val answer = cells[8]
        val before = cells[7]
        val pool = buildList {
            allMoves.forEach { add(math.apply(it, before)); add(math.apply(it, answer)) }
            add(before)
            add(cells[5])
            repeat(2) { add(toggled(answer, random)) }
        }
        val distractors = mutableListOf<DoubleArray>()
        val close = List(3) { toggled(answer, random) }
        for (option in (if (subtle) close + pool.shuffled(random) else pool.shuffled(random))) {
            if (option.sum() == 0.0) continue
            if ((distractors + listOf(answer)).any { math.mismatch(it, option) == 0.0 }) continue
            distractors += option
            if (distractors.size == 3) break
        }
        if (distractors.size < 3) return null
        val at = random.nextInt(4)
        val options = distractors.toMutableList().apply { add(at, answer) }
        return MatrixPuzzle(cells, options, at, rule)
    }

    /** The grid with one square next to the shape added. */
    private fun toggled(grid: DoubleArray, random: Random): DoubleArray {
        val out = grid.copyOf()
        val edge = (0 until GridMath.CELLS).filter { i ->
            out[i] == 0.0 && listOf(i - N, i + N).plus(if (i % N > 0) listOf(i - 1) else emptyList())
                .plus(if (i % N < N - 1) listOf(i + 1) else emptyList())
                .any { it in 0 until GridMath.CELLS && out[it] == 1.0 }
        }
        if (edge.isNotEmpty()) out[edge.random(random)] = 1.0
        return out
    }
}

/** Pattern building: part of a grid is given, and the rest follows from [rule]. */
enum class BuildRule(val op: GridOp, val rows: IntRange, val cols: IntRange, val title: String) {
    MIRROR(GridOp.MIRROR_FILL, 0 until N, 0 until N / 2, "Mirror the left half onto the right"),
    HALF_TURN(GridOp.HALF_TURN_FILL, 0 until N / 2, 0 until N, "Make it look the same upside down"),
    QUARTER(GridOp.QUARTER_FILL, 0 until N / 2, 0 until N / 2, "Mirror the corner both ways"),
    TILE(GridOp.TILE_FILL, 0 until N / 2, 0 until N / 2, "Repeat the corner as a tile"),
}

class BuildPuzzle(val rule: BuildRule, val given: DoubleArray, val answer: DoubleArray) {
    /** Cells inside the given part; the player fills everything else. */
    fun isGiven(index: Int) = index / N in rule.rows && index % N in rule.cols
}

object BuildMaker {
    /** [fill] is the share of the given part that is filled in; more squares make more to copy. */
    fun make(math: GridMath, random: Random, rule: BuildRule, fill: Double = 0.4): BuildPuzzle {
        val area = rule.rows.count() * rule.cols.count()
        while (true) {
            val given = blob(random, (area * fill).toInt() + random.nextInt(2), rule.rows, rule.cols)
            val answer = math.apply(rule.op, given)
            // A figure already symmetric in the given part teaches nothing; ask for one that isn't.
            val rowsUsed = (0 until GridMath.CELLS).filter { given[it] == 1.0 }.map { it / N }.distinct().size
            val colsUsed = (0 until GridMath.CELLS).filter { given[it] == 1.0 }.map { it % N }.distinct().size
            if (rowsUsed >= 2 && colsUsed >= 2) return BuildPuzzle(rule, given, answer)
        }
    }
}
