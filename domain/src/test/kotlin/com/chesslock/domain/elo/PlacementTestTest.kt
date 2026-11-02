package com.chesslock.domain.elo

import com.chesslock.domain.config.AppConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlacementTestTest {
    private val config = AppConfig.DEFAULT

    private fun run(results: List<Boolean>): PlacementTest =
        PlacementTest(config).also { t -> results.forEach { t.record(it) } }

    @Test
    fun startsAtConfiguredStartingElo() {
        val t = PlacementTest(config)
        assertEquals(1000.0, t.targetRating, 1e-9)
        assertEquals(1000, t.estimatedElo())
        assertFalse(t.isComplete)
    }

    @Test
    fun firstStepsShrinkByDecay() {
        val t = PlacementTest(config)
        t.record(true)
        assertEquals(1300.0, t.targetRating, 1e-9)
        t.record(false)
        assertEquals(1300.0 - 225.0, t.targetRating, 1e-9)
        t.record(true)
        assertEquals(1075.0 + 168.75, t.targetRating, 1e-9)
    }

    @Test
    fun stepNeverDropsBelowMinimum() {
        val t = run(List(8) { true })
        val before = t.targetRating
        t.record(true)
        assertEquals(before + config.placementStepMin, t.targetRating, 1e-9)
    }

    @Test
    fun completesAfterConfiguredNumberOfPuzzles() {
        val t = run(List(9) { true })
        assertFalse(t.isComplete)
        t.record(true)
        assertTrue(t.isComplete)
        assertEquals(10, t.answered)
    }

    @Test(expected = IllegalStateException::class)
    fun recordingAfterCompletionFails() {
        run(List(10) { true }).record(true)
    }

    @Test
    fun solvingEverythingGivesAHighElo() {
        // 1000 + 300 + 225 + 168.75 + 126.5625 + 94.921875 + 5 * 75 = 2290.23
        assertEquals(2290, run(List(10) { true }).estimatedElo())
    }

    @Test
    fun failingEverythingIsClampedToTheFloor() {
        assertEquals(config.eloFloor, run(List(10) { false }).estimatedElo())
    }

    @Test
    fun alternatingResultsSettleNearTheStart() {
        val results = List(10) { it % 2 == 0 }
        val elo = run(results).estimatedElo()
        assertTrue("elo=$elo", elo in 1100..1250)
    }

    @Test
    fun ratingIsCappedAtMaximum() {
        val custom = config.copy(placementMaxRating = 1500)
        val t = PlacementTest(custom)
        repeat(custom.placementPuzzleCount) { t.record(true) }
        assertEquals(1500, t.estimatedElo())
        assertEquals(1500.0, t.targetRating, 1e-9)
    }

    @Test
    fun targetNeverGoesBelowFloorDuringTheTest() {
        val t = PlacementTest(config)
        repeat(10) {
            t.record(false)
            assertTrue(t.targetRating >= config.eloFloor)
        }
    }

    @Test
    fun lengthComesFromConfig() {
        val short = PlacementTest(config.copy(placementPuzzleCount = 3))
        repeat(3) { short.record(true) }
        assertTrue(short.isComplete)
    }
}
