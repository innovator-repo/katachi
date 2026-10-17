package app.katachi.core

import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt
import kotlin.random.Random

data class Vec(val x: Double, val y: Double) {
    operator fun plus(o: Vec) = Vec(x + o.x, y + o.y)
    operator fun minus(o: Vec) = Vec(x - o.x, y - o.y)
    operator fun times(k: Double) = Vec(x * k, y * k)
    fun cross(o: Vec) = x * o.y - y * o.x
    fun dot(o: Vec) = x * o.x + y * o.y
    val length get() = hypot(x, y)

    /** Rotated by [eighths] × 45°, counter-clockwise in maths axes (clockwise on screen, where y points down). */
    fun turned(eighths: Int): Vec {
        val k = Math.floorMod(eighths, 8)
        val c = COS[k]
        val s = SIN[k]
        return Vec(x * c - y * s, x * s + y * c)
    }

    companion object {
        val ZERO = Vec(0.0, 0.0)
        private val H = sqrt(0.5)
        private val COS = doubleArrayOf(1.0, H, 0.0, -H, -1.0, -H, 0.0, H)
        private val SIN = doubleArrayOf(0.0, H, 1.0, H, 0.0, -H, -1.0, -H)
    }
}

/**
 * The seven classic pieces, in units where a small triangle has legs of 1:
 * the whole set covers area 8 (the 4 × 4 square cut in half each way).
 * Outlines are centred on their centroid so a piece turns about its middle.
 */
enum class Shape(raw: List<Vec>) {
    LARGE(listOf(Vec(0.0, 0.0), Vec(2.0, 0.0), Vec(0.0, 2.0))),
    MEDIUM(listOf(Vec(0.0, 0.0), Vec(2.0, 0.0), Vec(1.0, 1.0))),
    SMALL(listOf(Vec(0.0, 0.0), Vec(1.0, 0.0), Vec(0.0, 1.0))),
    SQUARE(listOf(Vec(0.0, 0.0), Vec(1.0, 0.0), Vec(1.0, 1.0), Vec(0.0, 1.0))),
    PARALLELOGRAM(listOf(Vec(0.0, 0.0), Vec(1.0, 0.0), Vec(2.0, 1.0), Vec(1.0, 1.0)));

    val outline: List<Vec> = raw.let { pts ->
        val c = Vec(pts.sumOf { it.x } / pts.size, pts.sumOf { it.y } / pts.size)
        pts.map { it - c }
    }

    /** Only the parallelogram changes when turned over; the others match a rotation of themselves. */
    val canFlip get() = this == PARALLELOGRAM

    companion object {
        val SET = listOf(LARGE, LARGE, MEDIUM, SMALL, SMALL, SQUARE, PARALLELOGRAM)
    }
}

/** Where a piece sits: turned by [turn] eighths, optionally turned over, centre at [at]. */
data class Pose(val turn: Int = 0, val flipped: Boolean = false, val at: Vec = Vec.ZERO)

fun Shape.polygon(pose: Pose): List<Vec> {
    val pts = outline.map { p -> (if (pose.flipped) Vec(-p.x, p.y) else p).turned(pose.turn) + pose.at }
    // Keep every polygon counter-clockwise so edge and containment tests need no sign juggling.
    return if (pose.flipped) pts.reversed() else pts
}

data class Placed(val shape: Shape, val pose: Pose) {
    val polygon: List<Vec> get() = shape.polygon(pose)
}

/** A silhouette and the arrangement it was built from, with its corner at (0, 0). */
data class Tangram(val pieces: List<Placed>) {
    val width: Double = pieces.maxOf { p -> p.polygon.maxOf { it.x } }
    val height: Double = pieces.maxOf { p -> p.polygon.maxOf { it.y } }
    val vertices: List<Vec> get() = pieces.flatMap { it.polygon }
}

object Geometry {
    const val EPS = 1e-6

    /** True when two convex polygons share interior area; touching along an edge or a corner is fine. */
    fun overlaps(a: List<Vec>, b: List<Vec>): Boolean {
        for (poly in listOf(a, b)) {
            for (i in poly.indices) {
                val edge = poly[(i + 1) % poly.size] - poly[i]
                val axis = Vec(-edge.y, edge.x)
                val (aMin, aMax) = project(a, axis)
                val (bMin, bMax) = project(b, axis)
                if (aMax <= bMin + EPS * axis.length || bMax <= aMin + EPS * axis.length) return false
            }
        }
        return true
    }

    private fun project(poly: List<Vec>, axis: Vec): Pair<Double, Double> {
        var lo = Double.MAX_VALUE
        var hi = -Double.MAX_VALUE
        for (p in poly) {
            val d = p.dot(axis)
            lo = min(lo, d)
            hi = max(hi, d)
        }
        return lo to hi
    }

    /** Point strictly inside a counter-clockwise convex polygon; a point on an edge is in neither neighbour. */
    fun contains(poly: List<Vec>, p: Vec): Boolean {
        for (i in poly.indices) {
            val a = poly[i]
            val b = poly[(i + 1) % poly.size]
            if ((b - a).cross(p - a) <= EPS) return false
        }
        return true
    }

    /** Length of the stretch two polygons share along their edges. */
    fun contact(a: List<Vec>, b: List<Vec>): Double {
        var total = 0.0
        for (i in a.indices) {
            val p = a[i]
            val q = a[(i + 1) % a.size]
            val dir = q - p
            val len = dir.length
            for (j in b.indices) {
                val r = b[j]
                val s = b[(j + 1) % b.size]
                if (abs(dir.cross(s - r)) > EPS * len * (s - r).length) continue
                if (abs(dir.cross(r - p)) > EPS * len) continue
                val t1 = dir.dot(r - p) / len
                val t2 = dir.dot(s - p) / len
                total += max(0.0, min(len, max(t1, t2)) - max(0.0, min(t1, t2)))
            }
        }
        return total
    }
}

/**
 * Builds a silhouette that is certain to be solvable: the seven pieces are
 * snapped together edge to edge, one at a time, never overlapping, and the
 * union of the result is the puzzle.
 */
object TangramMaker {
    fun make(random: Random, compact: Boolean): Tangram {
        while (true) attempt(random, compact)?.let { return it }
    }

    private fun attempt(random: Random, compact: Boolean): Tangram? {
        val order = Shape.SET.shuffled(random)
        val placed = mutableListOf(Placed(order[0], Pose(turn = random.nextInt(8), flipped = order[0].canFlip && random.nextBoolean())))
        for (shape in order.drop(1)) {
            val options = candidates(shape, placed)
            if (options.isEmpty()) return null
            placed += choose(options, placed, random, compact)
        }
        return normalised(placed)
    }

    private fun candidates(shape: Shape, placed: List<Placed>): List<Placed> {
        val out = mutableListOf<Placed>()
        val polys = placed.map { it.polygon }
        for (turn in 0 until 8) {
            for (flipped in if (shape.canFlip) listOf(false, true) else listOf(false)) {
                val base = shape.polygon(Pose(turn, flipped))
                for (poly in polys) {
                    for (i in poly.indices) {
                        val a = poly[i]
                        val b = poly[(i + 1) % poly.size]
                        for (j in base.indices) {
                            val c = base[j]
                            val d = base[(j + 1) % base.size]
                            if (abs((b - a).cross(d - c)) > Geometry.EPS) continue
                            val moves = listOf(a - c, b - c, a - d, b - d, (a + b) * 0.5 - (c + d) * 0.5)
                            for (move in moves) {
                                val pose = Pose(turn, flipped, move)
                                val candidate = shape.polygon(pose)
                                if (polys.any { Geometry.overlaps(it, candidate) }) continue
                                if (polys.sumOf { Geometry.contact(it, candidate) } < 0.5) continue
                                out += Placed(shape, pose)
                            }
                        }
                    }
                }
            }
        }
        return out.distinctBy { listOf(it.pose.turn, it.pose.flipped, (it.pose.at.x * 1e4).toLong(), (it.pose.at.y * 1e4).toLong()) }
    }

    /**
     * Compact figures (blocks, houses) give the eye few clues and are the hard ones.
     * Otherwise the piece goes somewhere with a good length of shared edge, which keeps
     * figures readable rather than spiky.
     */
    private fun choose(options: List<Placed>, placed: List<Placed>, random: Random, compact: Boolean): Placed {
        val polys = placed.map { it.polygon }
        return if (compact) {
            val scored = options.map { it to boxArea(polys + listOf(it.polygon)) }
            val best = scored.minOf { it.second }
            scored.filter { it.second <= best + 0.5 }.random(random).first
        } else {
            val scored = options.map { o -> o to polys.sumOf { Geometry.contact(it, o.polygon) } }
            val cut = scored.map { it.second }.sorted()[scored.size / 2]
            scored.filter { it.second >= cut }.random(random).first
        }
    }

    private fun boxArea(polys: List<List<Vec>>): Double {
        val pts = polys.flatten()
        return (pts.maxOf { it.x } - pts.minOf { it.x }) * (pts.maxOf { it.y } - pts.minOf { it.y })
    }

    private fun normalised(placed: List<Placed>): Tangram {
        val pts = placed.flatMap { it.polygon }
        val corner = Vec(pts.minOf { it.x }, pts.minOf { it.y })
        return Tangram(placed.map { it.copy(pose = it.pose.copy(at = it.pose.at - corner)) })
    }
}

/**
 * Turns polygons into the 96 × 96 grids `tangram_error` compares. The grid covers
 * the silhouette's box plus a margin; cell centres sit slightly off the half-unit
 * lattice so a shared edge never counts for both pieces.
 */
class Raster(tangram: Tangram) {
    private val n = GridMath.RASTER
    private val origin: Vec
    private val cell: Double

    init {
        val size = max(tangram.width, tangram.height) + 2 * MARGIN
        cell = size / n
        origin = Vec((tangram.width - size) / 2, (tangram.height - size) / 2)
    }

    val target: DoubleArray = cover(tangram.pieces.map { it.polygon })

    fun cover(polygons: List<List<Vec>>): DoubleArray {
        val out = DoubleArray(n * n)
        for (poly in polygons) {
            val xs = poly.map { it.x }
            val ys = poly.map { it.y }
            val c0 = max(0, ((xs.min() - origin.x) / cell).toInt() - 1)
            val c1 = min(n - 1, ((xs.max() - origin.x) / cell).toInt() + 1)
            val r0 = max(0, ((ys.min() - origin.y) / cell).toInt() - 1)
            val r1 = min(n - 1, ((ys.max() - origin.y) / cell).toInt() + 1)
            for (r in r0..r1) for (c in c0..c1) {
                val p = Vec(origin.x + (c + OFFSET) * cell, origin.y + (r + OFFSET) * cell)
                if (Geometry.contains(poly, p)) out[r * n + c] += 1.0
            }
        }
        return out
    }

    companion object {
        const val MARGIN = 0.5
        private const val OFFSET = 0.5137

        /** Below this share of wrong area the figure counts as solved; one small triangle is 6.25%. */
        const val SOLVED = 0.03
    }
}

object Snap {
    /** How close, in tangram units, a corner has to be to another corner to jump onto it. */
    const val REACH = 0.3

    /** Moves [piece] so its nearest corner lands on the nearest anchor, if one is within reach. */
    fun snap(piece: Placed, anchors: List<Vec>): Placed {
        var best: Vec? = null
        var bestDistance = REACH
        for (v in piece.polygon) for (a in anchors) {
            val d = (a - v).length
            if (d < bestDistance) {
                bestDistance = d
                best = a - v
            }
        }
        return best?.let { piece.copy(pose = piece.pose.copy(at = piece.pose.at + it)) } ?: piece
    }
}
