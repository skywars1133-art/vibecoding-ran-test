package com.chesslock.chess

import com.github.bhlangonijr.chesslib.Board
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChessPositionTest {
    @Test
    fun `initial position exposes legal UCI moves`() {
        val position = ChessPosition(Board().fen)

        assertTrue(position.legalMoves.contains("e2e4"))
        assertFalse(position.legalMoves.contains("e2e5"))
        assertEquals(20, position.legalMoves.size)
    }

    @Test
    fun `playing a legal move updates pieces and side to move`() {
        val position = ChessPosition(Board().fen)

        assertTrue(position.play("e2e4"))
        assertTrue(position.pieces.any { it.square == "e4" && it.isWhite })
        assertFalse(position.pieces.any { it.square == "e2" })
        assertTrue(position.legalMoves.contains("e7e5"))
        assertFalse(position.play("e2e5"))
    }

    @Test
    fun `detects checkmate`() {
        val position = ChessPosition(Board().fen)
        listOf("f2f3", "e7e5", "g2g4", "d8h4").forEach { assertTrue(position.play(it)) }

        assertTrue(position.isMated)
    }

    @Test
    fun `supports UCI promotion moves`() {
        val position = ChessPosition("7k/P7/8/8/8/8/8/7K w - - 0 1")

        assertTrue(position.legalMoves.contains("a7a8q"))
        assertTrue(position.play("a7a8q"))
        assertTrue(position.pieces.any { it.square == "a8" && it.symbol == "♛" && it.isWhite })
    }
}
