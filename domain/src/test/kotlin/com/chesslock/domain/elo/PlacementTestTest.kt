package com.chesslock.domain.elo

import com.chesslock.domain.config.AppConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlacementTestTest {
    private val config = AppConfig.DEFAULT

    private fun run(results: List<Pair<Int, Boolean>>, placementConfig: AppConfig = config): PlacementTest =
        PlacementTest(placementConfig).also { test ->
            results.forEach { (rating, solved) -> test.record(solved, rating) }
        }

    @Test
    fun startsAtConfiguredStartingElo() {
        val t = PlacementTest(config)
        assertEquals(1000.0, t.targetRating, 1e-9)
        assertEquals(1000, t.estimatedElo())
        assertFalse(t.isComplete)
    }

    @Test
    fun eachAnswerUpdatesTheEstimateFromTheActualPuzzleRating() {
        val t = PlacementTest(config)
        t.record(solved = true, puzzleRating = 1400)
        assertEquals(1081.81818181818, t.targetRating, 1e-9)
        t.record(solved = false, puzzleRating = 1400)
        assertEquals(1069.38268810037, t.targetRating, 1e-9)
    }

    @Test
    fun theSameResultOnAHarderPuzzleMovesTheEstimateMuchMore() {
        val hard = run(listOf(1400 to true))
        val easy = run(listOf(600 to true))
        assertTrue("hard=${hard.estimatedElo()} easy=${easy.estimatedElo()}", hard.estimatedElo() > easy.estimatedElo())
    }

    @Test
    fun completesAfterConfiguredNumberOfPuzzles() {
        val t = run(List(9) { 1000 to true })
        assertFalse(t.isComplete)
        t.record(solved = true, puzzleRating = 1000)
        assertTrue(t.isComplete)
        assertEquals(10, t.answered)
    }

    @Test(expected = IllegalStateException::class)
    fun recordingAfterCompletionFails() {
        run(List(10) { 1000 to true }).record(solved = true, puzzleRating = 1000)
    }

    @Test
    fun solvingHardPuzzlesRaisesTheEstimate() {
        // 10 wins against 1400-rated puzzles converge near 1552, not the old staircase's 2290.
        assertEquals(1552, run(List(10) { 1400 to true }).estimatedElo())
    }

    @Test
    fun solvingEasyPuzzlesOnlyRaisesTheEstimateSlightly() {
        assertEquals(1069, run(List(10) { 600 to true }).estimatedElo())
    }

    @Test
    fun failingEverythingIsClampedToTheFloor() {
        assertEquals(config.eloFloor, run(List(10) { 400 to false }).estimatedElo())
    }

    @Test
    fun alternatingAroundTheTrueRatingSettlesNearIt() {
        val results = List(10) { index -> (if (index % 2 == 0) 1300 else 1100) to (index % 2 == 0) }
        val elo = run(results).estimatedElo()
        assertTrue("elo=$elo", elo in 1050..1200)
    }

    @Test
    fun ratingIsCappedAtMaximum() {
        val custom = config.copy(eloCeiling = 1500)
        val t = run(List(10) { 2000 to true }, custom)
        assertEquals(1500, t.estimatedElo())
        assertEquals(1500.0, t.targetRating, 1e-9)
    }

    @Test
    fun targetNeverGoesBelowFloorDuringTheTest() {
        val t = PlacementTest(config)
        repeat(10) {
            t.record(solved = false, puzzleRating = 400)
            assertTrue(t.targetRating >= config.eloFloor)
        }
    }

    @Test
    fun lengthComesFromConfig() {
        val short = PlacementTest(config.copy(placementPuzzleCount = 3))
        repeat(3) { short.record(solved = true, puzzleRating = 1000) }
        assertTrue(short.isComplete)
    }
}
