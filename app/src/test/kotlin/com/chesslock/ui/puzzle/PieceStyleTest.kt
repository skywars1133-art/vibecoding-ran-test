package com.chesslock.ui.puzzle

import com.chesslock.chess.ChessPieceType
import com.chesslock.data.PieceStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PieceStyleTest {
    @Test
    fun `every style round-trips through its storage key`() {
        PieceStyle.entries.forEach { style ->
            assertEquals(style, PieceStyle.fromStorageKey(style.storageKey))
        }
    }

    @Test
    fun `missing or unknown storage key falls back to classic`() {
        assertEquals(PieceStyle.CLASSIC, PieceStyle.fromStorageKey(null))
        assertEquals(PieceStyle.CLASSIC, PieceStyle.fromStorageKey("unknown"))
    }

    @Test
    fun `every text style has a glyph for every piece type`() {
        PieceStyle.entries
            .filter { it.artwork(ChessPieceType.KING, isWhite = true) == null }
            .forEach { style ->
                ChessPieceType.entries.forEach { type ->
                    assertFalse(type.glyph(style).isNullOrEmpty())
                }
            }
    }

    @Test
    fun `artwork style has a drawable for every piece type and colour`() {
        ChessPieceType.entries.forEach { type ->
            assertNotNull(PieceStyle.CBURNETT.artwork(type, isWhite = true))
            assertNotNull(PieceStyle.CBURNETT.artwork(type, isWhite = false))
        }
    }

    @Test
    fun `text styles do not use artwork`() {
        val textStyles = listOf(PieceStyle.CLASSIC, PieceStyle.OUTLINE, PieceStyle.LETTERS)
        textStyles.forEach { style ->
            assertNull(style.artwork(ChessPieceType.PAWN, isWhite = true))
        }
        assertNull(ChessPieceType.PAWN.glyph(PieceStyle.CBURNETT))
    }

    @Test
    fun `artwork pairs differ between white and black`() {
        assertTrue(
            PieceStyle.CBURNETT.artwork(ChessPieceType.KING, isWhite = true) !=
                PieceStyle.CBURNETT.artwork(ChessPieceType.KING, isWhite = false),
        )
    }

    @Test
    fun `letters style uses piece letters`() {
        assertEquals("K", ChessPieceType.KING.glyph(PieceStyle.LETTERS))
        assertEquals("Q", ChessPieceType.QUEEN.glyph(PieceStyle.LETTERS))
        assertEquals("R", ChessPieceType.ROOK.glyph(PieceStyle.LETTERS))
        assertEquals("B", ChessPieceType.BISHOP.glyph(PieceStyle.LETTERS))
        assertEquals("N", ChessPieceType.KNIGHT.glyph(PieceStyle.LETTERS))
        assertEquals("P", ChessPieceType.PAWN.glyph(PieceStyle.LETTERS))
    }
}
