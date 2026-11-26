package com.chesslock.placement

import com.chesslock.domain.config.AppConfig
import com.chesslock.domain.elo.EloCalculator
import com.chesslock.domain.elo.PlacementTest
import com.chesslock.domain.puzzle.LichessPuzzleCsv
import com.chesslock.domain.puzzle.PuzzleSelector
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.Random
import kotlin.math.roundToInt

/**
 * Reproducible tuning check for the adaptive placement flow against the actual offline stock.
 * Each simulated player has a true rating; their answers are sampled from the ELO expectation at
 * the puzzle rating chosen by the same selector the app uses.
 */
class PlacementTuningTest {
    private val config = AppConfig.DEFAULT

    private val stock by lazy {
        val errors = mutableListOf<String>()
        val puzzles = LichessPuzzleCsv.parseAll(
            File("src/main/assets/builtin_puzzles.csv").readLines().asSequence(),
        ) { line, message -> errors += "line $line: $message" }
        check(errors.isEmpty()) { errors.joinToString("; ") }
        puzzles
    }

    private fun simulate(trueElo: Int, seed: Int): Int {
        val test = PlacementTest(config)
        val usedIds = mutableSetOf<String>()
        val random = Random(seed.toLong())
        repeat(config.placementPuzzleCount) {
            val puzzle = PuzzleSelector.pick(
                candidates = stock,
                targetRating = test.targetRating.roundToInt(),
                solvedIds = usedIds,
                random = random,
                config = config,
            ) ?: return@repeat
            usedIds += puzzle.id
            val solved = random.nextDouble() < EloCalculator.expectedScore(trueElo, puzzle.rating, config)
            test.record(solved = solved, puzzleRating = puzzle.rating)
        }
        return test.estimatedElo()
    }

    private fun meanElo(trueElo: Int, seeds: Int = 40): Int =
        (1..seeds).map { simulate(trueElo, it * 7919) }.average().roundToInt()

    @Test
    fun placementTracksBeginnerThroughAdvancedPlayers() {
        val expectedRanges = mapOf(
            800 to 650..1050,
            1200 to 1000..1400,
            1600 to 1350..1800,
            2000 to 1700..2250,
            2400 to 2000..2600,
        )
        expectedRanges.forEach { (trueElo, range) ->
            val mean = meanElo(trueElo)
            assertTrue("trueElo=$trueElo mean=$mean expected=$range", mean in range)
        }
    }

    @Test
    fun placementNeverReturnsAnOutOfRangeRating() {
        listOf(800, 1200, 1600, 2000, 2400).forEach { trueElo ->
            repeat(40) { seed ->
                val elo = simulate(trueElo, seed * 104729 + 11)
                assertTrue("trueElo=$trueElo elo=$elo", elo in config.eloFloor..config.eloCeiling)
            }
        }
    }
}
