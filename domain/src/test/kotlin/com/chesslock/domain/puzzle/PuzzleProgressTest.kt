package com.chesslock.domain.puzzle

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PuzzleProgressTest {
    private fun puzzle(vararg moves: String) = Puzzle(
        id = "T1",
        fen = "8/8/8/8/8/8/8/8 b - - 0 1",
        moves = moves.toList(),
        rating = 1500,
        ratingDeviation = 80,
        popularity = 90,
        nbPlays = 1000,
        themes = emptyList(),
        gameUrl = "",
        openingTags = emptyList(),
    )

    private val noAltMate = MateChecker { _, _ -> false }

    @Test
    fun startPlaysOpponentsFirstMove() {
        val progress = PuzzleProgress(puzzle("a1a2", "b1b2"), noAltMate)
        assertEquals("a1a2", progress.start())
        assertEquals(listOf("a1a2"), progress.movesPlayed)
        assertEquals("b1b2", progress.expectedMove)
    }

    @Test
    fun singleMovePuzzleIsSolvedByTheCorrectMove() {
        val progress = PuzzleProgress(puzzle("a1a2", "b1b2"), noAltMate)
        progress.start()
        val result = progress.submit("b1b2") as MoveResult.Correct
        assertTrue(result.solved)
        assertNull(result.reply)
        assertFalse(result.alternativeMate)
        assertTrue(progress.solved)
        assertNull(progress.expectedMove)
    }

    @Test
    fun multiMovePuzzlePlaysOpponentRepliesAutomatically() {
        val progress = PuzzleProgress(puzzle("a1a2", "b1b2", "c1c2", "d1d2"), noAltMate)
        progress.start()
        val first = progress.submit("b1b2") as MoveResult.Correct
        assertFalse(first.solved)
        assertEquals("c1c2", first.reply)
        assertEquals("d1d2", progress.expectedMove)
        val second = progress.submit("d1d2") as MoveResult.Correct
        assertTrue(second.solved)
        assertEquals(listOf("a1a2", "b1b2", "c1c2", "d1d2"), progress.movesPlayed)
    }

    @Test
    fun wrongMoveLeavesPositionUnchanged() {
        val progress = PuzzleProgress(puzzle("a1a2", "b1b2", "c1c2", "d1d2"), noAltMate)
        progress.start()
        assertEquals(MoveResult.Wrong, progress.submit("h1h2"))
        assertEquals(listOf("a1a2"), progress.movesPlayed)
        assertEquals("b1b2", progress.expectedMove)
        assertFalse(progress.solved)
        // The player can still find the right move afterwards.
        assertTrue(progress.submit("b1b2") is MoveResult.Correct)
    }

    @Test
    fun alternativeMatingMoveIsAcceptedAndEndsThePuzzle() {
        val asked = mutableListOf<Pair<List<String>, String>>()
        val checker = MateChecker { soFar, candidate ->
            asked += soFar to candidate
            candidate == "g1g8"
        }
        val progress = PuzzleProgress(puzzle("a1a2", "b1b2", "c1c2", "d1d2"), checker)
        progress.start()
        val result = progress.submit("g1g8") as MoveResult.Correct
        assertTrue(result.solved)
        assertTrue(result.alternativeMate)
        assertNull(result.reply)
        assertTrue(progress.solved)
        assertEquals(listOf(listOf("a1a2") to "g1g8"), asked)
    }

    @Test
    fun nonMatingAlternativeIsStillWrong() {
        val progress = PuzzleProgress(puzzle("a1a2", "b1b2"), MateChecker { _, c -> c == "g1g8" })
        progress.start()
        assertEquals(MoveResult.Wrong, progress.submit("g1g7"))
    }

    @Test
    fun correctMoveDoesNotAskTheMateChecker() {
        val progress = PuzzleProgress(
            puzzle("a1a2", "b1b2"),
            MateChecker { _, _ -> error("must not be called for the expected move") },
        )
        progress.start()
        assertTrue(progress.submit("b1b2") is MoveResult.Correct)
    }

    @Test
    fun promotionMoveMustMatchPieceLetter() {
        val progress = PuzzleProgress(puzzle("a1a2", "e7e8q"), noAltMate)
        progress.start()
        assertEquals(MoveResult.Wrong, progress.submit("e7e8n"))
        assertTrue(progress.submit("E7E8Q") is MoveResult.Correct)
    }

    @Test(expected = IllegalStateException::class)
    fun submitBeforeStartFails() {
        PuzzleProgress(puzzle("a1a2", "b1b2"), noAltMate).submit("b1b2")
    }

    @Test(expected = IllegalStateException::class)
    fun submitAfterSolvedFails() {
        val progress = PuzzleProgress(puzzle("a1a2", "b1b2"), noAltMate)
        progress.start()
        progress.submit("b1b2")
        progress.submit("b1b2")
    }

    @Test(expected = IllegalArgumentException::class)
    fun puzzleNeedsAtLeastTwoMovesAndEvenCount() {
        puzzle("a1a2", "b1b2", "c1c2")
    }
}
