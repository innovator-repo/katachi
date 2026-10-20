package app.katachi.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import kotlin.random.Random

class CoreTest {
    private val math = KotlinMath
    private val n = GridMath.N

    /** An L in the top-left corner: (0,0) (1,0) (2,0) (2,1), as in lume/host_check.c. */
    private val l = DoubleArray(GridMath.CELLS).also { it[0] = 1.0; it[6] = 1.0; it[12] = 1.0; it[13] = 1.0 }

    @Test fun gridOpsMatchTheLumeCheck() {
        val turned = math.apply(GridOp.TURN, l)
        assertEquals(3.0, turned[3] + turned[4] + turned[5], 0.0)
        assertEquals(1.0, turned[1 * n + 3], 0.0)
        assertEquals(1.0, math.apply(GridOp.MIRROR, l)[2 * n + 4], 0.0)
        assertEquals(1.0, math.apply(GridOp.RIGHT, l)[1], 0.0)
        assertEquals(32.0, math.apply(GridOp.INVERT, l).sum(), 0.0)
        val quarter = math.apply(GridOp.QUARTER_FILL, l)
        assertEquals(16.0, quarter.sum(), 0.0)
        assertEquals(1.0, quarter[35], 0.0)
        assertEquals(16.0, math.apply(GridOp.TILE_FILL, l).sum(), 0.0)
        assertEquals(0.0, math.mismatch(l, l), 0.0)
        assertEquals(4.0, math.mismatch(l, GridMath.EMPTY), 0.0)
        assertEquals(0.0, math.mismatch(l, (1..4).fold(l) { g, _ -> math.apply(GridOp.TURN, g) }), 0.0)
        assertEquals(1.0, math.apply(GridOp.DOWN, math.apply(GridOp.RIGHT, l))[1 * n + 1], 0.0)
    }

    @Test fun tangramErrorMatchesTheLumeCheck() {
        val size = GridMath.RASTER * GridMath.RASTER
        val target = DoubleArray(size) { if (it < size / 2) 1.0 else 0.0 }
        val cover = target.copyOf()
        assertEquals(0.0, math.tangramError(target, cover), 1e-12)
        cover[0] = 0.0
        cover[size - 1] = 2.0
        assertEquals(3.0 / (size / 2), math.tangramError(target, cover), 1e-9)
    }

    @Test fun everyTangramIsTheFullSetWithNoOverlaps() {
        repeat(60) { seed ->
            val t = TangramMaker.make(Random(seed), compact = seed % 2 == 0)
            assertEquals(Shape.SET.sorted(), t.pieces.map { it.shape }.sorted())
            val polys = t.pieces.map { it.polygon }
            for (i in polys.indices) for (j in i + 1 until polys.size) {
                assertFalse("seed $seed pieces $i and $j overlap", Geometry.overlaps(polys[i], polys[j]))
            }
            assertTrue(t.vertices.all { it.x > -1e-9 && it.y > -1e-9 })
            // The raster sees the whole set's area of 8, give or take its resolution.
            val raster = Raster(t)
            val cellArea = (maxOf(t.width, t.height) + 2 * Raster.MARGIN).let { it * it } / (GridMath.RASTER * GridMath.RASTER)
            assertEquals(8.0, raster.target.sum() * cellArea, 0.5)
            assertTrue(raster.target.all { it <= 1.0 })
        }
    }

    @Test fun theBuiltArrangementSolvesItAndAMisplacedPieceDoesNot() {
        val t = TangramMaker.make(Random(7), compact = false)
        val raster = Raster(t)
        assertEquals(0.0, math.tangramError(raster.target, raster.cover(t.pieces.map { it.polygon })), 1e-12)
        val nudged = t.pieces.mapIndexed { i, p ->
            if (i == 0) p.copy(pose = p.pose.copy(at = p.pose.at + Vec(0.6, 0.0))) else p
        }
        assertTrue(math.tangramError(raster.target, raster.cover(nudged.map { it.polygon })) > Raster.SOLVED)
    }

    @Test fun aPieceDroppedNearItsPlaceSnapsOntoIt() {
        val t = TangramMaker.make(Random(11), compact = true)
        val piece = t.pieces[3]
        val dropped = piece.copy(pose = piece.pose.copy(at = piece.pose.at + Vec(0.12, -0.1)))
        val snapped = Snap.snap(dropped, t.vertices)
        assertEquals(piece.pose.at.x, snapped.pose.at.x, 1e-9)
        assertEquals(piece.pose.at.y, snapped.pose.at.y, 1e-9)
        val far = piece.copy(pose = piece.pose.copy(at = piece.pose.at + Vec(5.0, 5.0)))
        assertEquals(far, Snap.snap(far, t.vertices))
    }

    @Test fun flippingKeepsPolygonsCounterClockwise() {
        for (shape in Shape.entries) for (turn in 0 until 8) for (flipped in listOf(false, true)) {
            val poly = shape.polygon(Pose(turn, flipped, Vec(1.0, 2.0)))
            assertTrue(Geometry.contains(poly, Vec(1.0, 2.0)))
        }
    }

    @Test fun matrixPuzzlesHaveOneRightAndThreeDistinctOptions() {
        repeat(40) { seed ->
            for (level in 0..2) {
                val p = MatrixMaker.make(math, Random(seed), level)
                assertEquals(9, p.cells.size)
                assertEquals(4, p.options.size)
                assertEquals(0.0, math.mismatch(p.options[p.answer], p.cells[8]), 0.0)
                for (i in 0 until 4) for (j in i + 1 until 4) {
                    assertNotEquals("seed $seed level $level", 0.0, math.mismatch(p.options[i], p.options[j]), 0.0)
                }
            }
        }
    }

    @Test fun builtAnswersKeepTheGivenPartAndFollowTheRule() {
        repeat(30) { seed ->
            for (rule in BuildRule.entries) {
                val p = BuildMaker.make(math, Random(seed), rule)
                for (i in 0 until GridMath.CELLS) if (p.isGiven(i)) assertEquals(p.given[i], p.answer[i], 0.0)
                val again = when (rule) {
                    BuildRule.MIRROR -> math.apply(GridOp.MIRROR, p.answer)
                    BuildRule.HALF_TURN -> math.apply(GridOp.TURN, math.apply(GridOp.TURN, p.answer))
                    BuildRule.QUARTER -> math.apply(GridOp.FLIP, math.apply(GridOp.MIRROR, p.answer))
                    BuildRule.TILE -> math.apply(GridOp.RIGHT, math.apply(GridOp.RIGHT, math.apply(GridOp.RIGHT, p.answer)))
                }
                assertEquals(0.0, math.mismatch(again, p.answer), 0.0)
                assertTrue((0 until GridMath.CELLS).any { !p.isGiven(it) && p.answer[it] == 1.0 })
            }
        }
    }

    @Test fun aDayAlwaysBringsTheSameSetAndTheNextDayADifferentOne() {
        val day = LocalDate.of(2026, 10, 17)
        val a = DailySet(day, math)
        val b = DailySet(day, math)
        val c = DailySet(day.plusDays(1), math)
        assertEquals(a.tangrams.map { it.tangram }, b.tangrams.map { it.tangram })
        assertEquals(a.matrices.map { it.answer }, b.matrices.map { it.answer })
        assertNotEquals(a.tangrams.map { it.tangram }, c.tangrams.map { it.tangram })
        assertEquals(listOf(3, 2, 1, 0, 0), a.tangrams.map { it.outlinesShown })
    }

    @Test fun historyCountsStreaksAndPace() {
        val today = LocalDate.of(2026, 10, 17)
        var h = History()
        for (back in 1L..3L) {
            val date = today.minusDays(back)
            for (kind in Kind.entries) {
                val count = when (kind) { Kind.TANGRAM -> DailySet.TANGRAMS; Kind.MATRIX -> DailySet.MATRICES; Kind.BUILD -> DailySet.BUILDS }
                for (i in 0 until count) h = h.withResult(date, Result(DailySet.id(kind, i), seconds = 60))
            }
        }
        assertEquals(3, h.streak(today))
        h = h.withResult(today, Result("t0", seconds = 30))
        assertEquals(3, h.streak(today))
        assertEquals(h, h.withResult(today, Result("t0", seconds = 99)))
        assertEquals((15 * 60 + 30) / 16.0, h.tangramPace(today)!!, 1e-9)
        assertEquals(h, Codec.decode(Codec.encode(h)))
    }

    @Test fun aGoodDayRaisesTheLevelByOne() {
        val today = LocalDate.of(2026, 10, 17)
        var h = History()
        assertEquals(1, h.level(today))
        val yesterday = today.minusDays(1)
        for (i in 0 until Difficulty.TO_LEVEL_UP - 1) h = h.withResult(yesterday, Result("x$i", 10))
        assertEquals(1, h.level(today))
        h = h.withResult(yesterday, Result("last", 10))
        assertEquals(2, h.level(today))
        // Today's own progress only counts from tomorrow.
        for (i in 0 until DailySet.TOTAL) h = h.withResult(today, Result("t$i", 10))
        assertEquals(2, h.level(today))
        assertEquals(3, h.level(today.plusDays(1)))
    }

    @Test fun difficultyOnlyEverGetsHarder() {
        for (level in 1 until Difficulty.MAX) {
            val now = Difficulty(level)
            val next = Difficulty(level + 1)
            for (i in 0 until DailySet.TANGRAMS) {
                assertTrue(next.tangramOutlines(i) <= now.tangramOutlines(i))
                assertTrue(!now.tangramCompact(i) || next.tangramCompact(i))
            }
            assertTrue(next.matrixKinds.sum() >= now.matrixKinds.sum())
            assertTrue(next.matrixMoves.containsAll(now.matrixMoves))
            assertTrue(next.matrixSizes.first >= now.matrixSizes.first)
            assertTrue(next.buildFill >= now.buildFill)
        }
    }

    @Test fun everyLevelMakesAFullSet() {
        val day = LocalDate.of(2026, 10, 17)
        for (level in listOf(1, 3, 5, 8, 12, 20, Difficulty.MAX)) {
            val set = DailySet(day, math, level)
            assertEquals(DailySet.TANGRAMS, set.tangrams.size)
            assertEquals(set.difficulty.matrixKinds.size, set.matrices.size)
            set.matrices.forEach { assertEquals(0.0, math.mismatch(it.options[it.answer], it.cells[8]), 0.0) }
        }
    }
}

class LumeTest {
    @Test fun floorModeLowersTheTargetToOne() {
        assertEquals(1, DailyTarget.of(true))
        assertEquals(5, DailyTarget.of(false))
    }

    @Test fun circadianPhasesFollowTheClock() {
        assertEquals(Phase.SLEEP, Phase.at(3))
        assertEquals(Phase.MORNING, Phase.at(6))
        assertEquals(Phase.MORNING, Phase.at(11))
        assertEquals(Phase.DAY, Phase.at(12))
        assertEquals(Phase.DAY, Phase.at(19))
        assertEquals(Phase.EVENING, Phase.at(20))
        assertEquals(Phase.SLEEP, Phase.at(22))
        assertEquals(Phase.SLEEP, Phase.at(0))
    }

    private val square = listOf(Vec(0.0, 0.0), Vec(1.0, 0.0), Vec(1.0, 1.0), Vec(0.0, 1.0))
    private fun shifted(dx: Double, dy: Double) = square.map { it + Vec(dx, dy) }

    @Test fun nearbyAlignedCornerAttracts() {
        val pull = Magnet.pull(shifted(1.05, 0.02), listOf(square), 0.2)!!
        assertEquals(-0.05, pull.x, 1e-9)
        assertEquals(-0.02, pull.y, 1e-9)
    }

    @Test fun farPieceIsLeftAlone() {
        assertEquals(null, Magnet.pull(shifted(1.5, 0.0), listOf(square), 0.2))
    }

    @Test fun pieceTurnedTooFarIsLeftAlone() {
        val c = Math.cos(Math.toRadians(30.0)); val s = Math.sin(Math.toRadians(30.0))
        val turned = square.map { Vec(it.x * c - it.y * s + 1.05, it.x * s + it.y * c) }
        assertEquals(null, Magnet.pull(turned, listOf(square), 0.2))
    }

    @Test fun ecoPayloadIsJson() {
        assertEquals("""{"solved":3,"target":5,"solveTimeMs":41000}""", EcoMetrics.payload(3, 5, 41000))
    }
}
