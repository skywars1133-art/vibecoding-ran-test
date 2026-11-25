package com.chesslock.ui.puzzle

import com.chesslock.chess.ChessPiece
import com.chesslock.chess.ChessPieceType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PieceMoveAnimationTest {

    private fun piece(
        square: String,
        type: ChessPieceType = ChessPieceType.PAWN,
        isWhite: Boolean = true,
    ) = ChessPiece(square, type, isWhite)

    @Test
    fun `a quiet move animates the mover from source to target`() {
        val before = listOf(piece("e2"), piece("e7", isWhite = false))
        val steps = buildPieceMoveSteps(before, listOf("e2e4"))

        assertEquals(1, steps.size)
        val step = steps.single()
        val mover = step.animatedPieces.single()
        assertEquals("e2", mover.fromSquare)
        assertEquals("e4", mover.toSquare)
        assertEquals(piece("e4"), mover.piece)
        assertNull(mover.capturedPiece)
        assertEquals(setOf("e7", "e4"), step.piecesAfter.map { it.square }.toSet())
        assertNull(step.piecesAfter.firstOrNull { it.square == "e2" })
    }

    @Test
    fun `a capture keeps the captured piece visible until the mover lands`() {
        val before = listOf(piece("e2"), piece("d3", isWhite = false))
        val step = buildPieceMoveSteps(before, listOf("e2d3")).single()
        val mover = step.animatedPieces.single()

        assertEquals(piece("d3", isWhite = false), mover.capturedPiece)
        assertTrue(step.piecesBefore.any { it == piece("d3", isWhite = false) })
        assertEquals(setOf("d3"), step.piecesAfter.map { it.square }.toSet())
        assertEquals(piece("d3"), step.piecesAfter.single())
    }

    @Test
    fun `promotion animates the promoted piece`() {
        val before = listOf(piece("e7"))
        val step = buildPieceMoveSteps(before, listOf("e7e8q")).single()

        assertEquals(piece("e8", ChessPieceType.QUEEN), step.animatedPieces.single().piece)
        assertEquals(piece("e8", ChessPieceType.QUEEN), step.piecesAfter.single())
    }

    @Test
    fun `castling animates the king and the rook together`() {
        val before = listOf(
            piece("e1", ChessPieceType.KING),
            piece("h1", ChessPieceType.ROOK),
            piece("e8", ChessPieceType.KING, isWhite = false),
        )
        val step = buildPieceMoveSteps(before, listOf("e1g1")).single()

        assertEquals(2, step.animatedPieces.size)
        assertTrue(step.animatedPieces.any { it.fromSquare == "e1" && it.toSquare == "g1" })
        assertTrue(step.animatedPieces.any { it.fromSquare == "h1" && it.toSquare == "f1" })
        assertEquals(
            setOf("g1", "f1", "e8"),
            step.piecesAfter.map { it.square }.toSet(),
        )
    }

    @Test
    fun `queenside castling moves the a-file rook`() {
        val before = listOf(
            piece("e8", ChessPieceType.KING, isWhite = false),
            piece("a8", ChessPieceType.ROOK, isWhite = false),
        )
        val step = buildPieceMoveSteps(before, listOf("e8c8")).single()

        assertTrue(step.animatedPieces.any { it.fromSquare == "a8" && it.toSquare == "d8" })
    }

    @Test
    fun `a player move and puzzle reply become two sequential steps`() {
        val before = listOf(piece("e2"), piece("e7", isWhite = false))
        val steps = buildPieceMoveSteps(before, listOf("e2e4", "e7e5"))

        assertEquals(2, steps.size)
        assertEquals("e2", steps[0].animatedPieces.single().fromSquare)
        assertEquals("e4", steps[0].animatedPieces.single().toSquare)
        assertEquals(setOf("e4", "e7"), steps[0].piecesAfter.map { it.square }.toSet())
        assertEquals("e7", steps[1].animatedPieces.single().fromSquare)
        assertEquals("e5", steps[1].animatedPieces.single().toSquare)
        assertEquals(setOf("e4", "e5"), steps[1].piecesAfter.map { it.square }.toSet())
    }

    @Test
    fun `uppercase and mixed case UCI moves are accepted`() {
        val before = listOf(piece("e2"))
        val step = buildPieceMoveSteps(before, listOf("E2E4")).single()

        assertNotNull(step.animatedPieces.single())
        assertEquals("e4", step.piecesAfter.single().square)
    }

    @Test
    fun `malformed or impossible moves produce no steps`() {
        val before = listOf(piece("e2"))
        assertTrue(buildPieceMoveSteps(before, listOf("e2")).isEmpty())
        assertTrue(buildPieceMoveSteps(before, listOf("z2z4")).isEmpty())
        assertTrue(buildPieceMoveSteps(before, listOf("e2e9")).isEmpty())
        assertTrue(buildPieceMoveSteps(before, listOf("e2e2")).isEmpty())
        assertTrue(buildPieceMoveSteps(before, listOf("d2d4")).isEmpty())
        assertTrue(buildPieceMoveSteps(before, listOf("e2e4x")).isEmpty())
    }

    @Test
    fun `display index mirrors files and ranks for the black player`() {
        assertEquals(4 to 6, squareDisplayIndex("e2", playerIsWhite = true))
        assertEquals(3 to 1, squareDisplayIndex("e2", playerIsWhite = false))
        assertEquals(0 to 0, squareDisplayIndex("a8", playerIsWhite = true))
        assertEquals(7 to 7, squareDisplayIndex("a8", playerIsWhite = false))
        assertNull(squareDisplayIndex("e", playerIsWhite = true))
        assertNull(squareDisplayIndex("i9", playerIsWhite = true))
    }
}
