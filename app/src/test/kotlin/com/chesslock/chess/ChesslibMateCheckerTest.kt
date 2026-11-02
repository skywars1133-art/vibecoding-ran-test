package com.chesslock.chess

import com.github.bhlangonijr.chesslib.Board
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChesslibMateCheckerTest {
    private val checker = ChesslibMateChecker(Board().fen)
    private val history = listOf("f2f3", "e7e5", "g2g4")

    @Test
    fun `accepts legal mating alternative`() {
        assertTrue(checker.deliversMate(history, "d8h4"))
    }

    @Test
    fun `rejects legal non mating move`() {
        assertFalse(checker.deliversMate(history, "d8e7"))
    }

    @Test
    fun `rejects illegal candidate`() {
        assertFalse(checker.deliversMate(history, "d8h6"))
    }

    @Test
    fun `rejects invalid history without accepting candidate`() {
        assertFalse(checker.deliversMate(listOf("e2e5"), "d8h4"))
    }
}
