package com.chesslock.domain.puzzle

import com.chesslock.domain.config.AppConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PuzzlePackPlannerTest {
    private val config = AppConfig.DEFAULT

    @Test
    fun `default bands run from 400 to 2800 in steps of 200`() {
        val centers = config.puzzlePackBandCenters()
        assertEquals(13, centers.size)
        assertEquals(400, centers.first())
        assertEquals(2800, centers.last())
        assertEquals((400..2800 step 200).toList(), centers)
    }

    @Test
    fun `band center is the nearest configured center`() {
        assertEquals(400, PuzzlePackPlanner.bandCenter(100, config))
        assertEquals(400, PuzzlePackPlanner.bandCenter(499, config))
        assertEquals(600, PuzzlePackPlanner.bandCenter(501, config))
        assertEquals(1000, PuzzlePackPlanner.bandCenter(987, config))
        assertEquals(1600, PuzzlePackPlanner.bandCenter(1650, config))
    }

    @Test
    fun `band center clamps below and above the generated range`() {
        assertEquals(400, PuzzlePackPlanner.bandCenter(-5000, config))
        assertEquals(2800, PuzzlePackPlanner.bandCenter(99_999, config))
    }

    @Test
    fun `a pack covers 300 points on both sides`() {
        val range = PuzzlePackPlanner.bandRange(1000, config)
        assertEquals(700, range.first)
        assertEquals(1299, range.last)
        assertTrue(PuzzlePackPlanner.contains(700, 1000, config))
        assertTrue(PuzzlePackPlanner.contains(1299, 1000, config))
        assertFalse(PuzzlePackPlanner.contains(699, 1000, config))
        assertFalse(PuzzlePackPlanner.contains(1300, 1000, config))
    }

    @Test
    fun `file names and download urls are deterministic`() {
        assertEquals("puzzles-1200.csv", PuzzlePackPlanner.fileName(1200))
        assertEquals(
            "https://example.test/releases/download/puzzle-packs/puzzles-1200.csv",
            PuzzlePackPlanner.packUrl("https://example.test/releases/download/puzzle-packs/", 1200),
        )
        assertEquals(
            "https://example.test/releases/download/puzzle-packs/puzzles-1200.csv?v=20400",
            PuzzlePackPlanner.packUrl("https://example.test/releases/download/puzzle-packs", 1200, 20400),
        )
    }

    @Test
    fun `sync is due when never successful or the interval elapsed`() {
        val interval = config.puzzlePackRefreshIntervalMs
        assertTrue(PuzzlePackPlanner.isDue(0, 1_000, interval))
        assertTrue(PuzzlePackPlanner.isDue(1_000, 1_000 + interval, interval))
        assertFalse(PuzzlePackPlanner.isDue(5_000, 5_000 + interval - 1, interval))
        assertFalse("clock moved backwards", PuzzlePackPlanner.isDue(5_000, 1_000, interval))
    }
}
