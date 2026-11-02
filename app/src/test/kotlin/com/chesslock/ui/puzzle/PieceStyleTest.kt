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
    fun `missing or unknown storage key falls back to the default`() {
        assertEquals(PieceStyle.DEFAULT, PieceStyle.fromStorageKey(null))
        assertEquals(PieceStyle.DEFAULT, PieceStyle.fromStorageKey("unknown"))
    }

    @Test
    fun `cburnett is the default artwork style`() {
        assertEquals(PieceStyle.CBURNETT, PieceStyle.DEFAULT)
    }

    @Test
    fun `every text style has a glyph for every piece type`() {
        PieceStyle.entries.filter { it.isText }.forEach { style ->
            ChessPieceType.entries.forEach { type ->
                assertFalse(type.glyph(style).isNullOrEmpty())
            }
        }
    }

    @Test
    fun `cburnett has a drawable for every piece type and colour`() {
        ChessPieceType.entries.forEach { type ->
            assertNotNull(PieceStyle.CBURNETT.artwork(type, isWhite = true))
            assertNotNull(PieceStyle.CBURNETT.artwork(type, isWhite = false))
        }
    }

    @Test
    fun `svg artwork styles load from their bundled asset folder`() {
        val svgStyles = PieceStyle.entries.filter { it.svgFolder != null }
        assertEquals(
            setOf(
                PieceStyle.CHESSNUT,
                PieceStyle.FANTASY,
                PieceStyle.CELTIC,
                PieceStyle.SPATIAL,
            ),
            svgStyles.toSet(),
        )
        svgStyles.forEach { style ->
            assertFalse(style.isText)
            assertEquals(style.storageKey, style.svgFolder)
            ChessPieceType.entries.forEach { type ->
                assertNull(style.artwork(type, isWhite = true))
                assertNull(type.glyph(style))
            }
        }
    }

    @Test
    fun `text styles do not use artwork`() {
        val textStyles = listOf(PieceStyle.CLASSIC, PieceStyle.OUTLINE, PieceStyle.LETTERS)
        textStyles.forEach { style ->
            assertTrue(style.isText)
            assertNull(style.artwork(ChessPieceType.PAWN, isWhite = true))
        }
        assertFalse(PieceStyle.CBURNETT.isText)
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
