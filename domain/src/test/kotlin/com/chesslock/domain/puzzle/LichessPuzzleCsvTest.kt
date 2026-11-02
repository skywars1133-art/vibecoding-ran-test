package com.chesslock.domain.puzzle

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LichessPuzzleCsvTest {
    // Synthetic row in the Lichess column layout; only the format matters for these tests.
    private val sample =
        "00008,r6k/pp2r2p/4Rp1Q/3p4/8/1N1P2R1/PqP2bPP/7K b - - 0 24," +
            "f2g3 e6e7 b2b1 b3c1 b1c1 h6c1,1913,75,95,5603," +
            "crushing hangingPiece long middlegame,https://lichess.org/787zsVup/black#48,Sicilian_Defense Sicilian_Defense_Alapin_Variation"

    @Test
    fun parsesAllColumns() {
        val p = LichessPuzzleCsv.parseLine(sample)
        assertEquals("00008", p.id)
        assertEquals("r6k/pp2r2p/4Rp1Q/3p4/8/1N1P2R1/PqP2bPP/7K b - - 0 24", p.fen)
        assertEquals(listOf("f2g3", "e6e7", "b2b1", "b3c1", "b1c1", "h6c1"), p.moves)
        assertEquals(1913, p.rating)
        assertEquals(75, p.ratingDeviation)
        assertEquals(95, p.popularity)
        assertEquals(5603, p.nbPlays)
        assertEquals(listOf("crushing", "hangingPiece", "long", "middlegame"), p.themes)
        assertEquals("https://lichess.org/787zsVup/black#48", p.gameUrl)
        assertEquals(listOf("Sicilian_Defense", "Sicilian_Defense_Alapin_Variation"), p.openingTags)
    }

    @Test
    fun openingTagsColumnMayBeMissingOrEmpty() {
        val withEmpty = sample.substringBeforeLast(",") + ","
        assertTrue(LichessPuzzleCsv.parseLine(withEmpty).openingTags.isEmpty())
        assertTrue(LichessPuzzleCsv.parseLine(sample.substringBeforeLast(",")).openingTags.isEmpty())
    }

    @Test
    fun sideToMoveGivesPlayerColor() {
        // FEN says black to move: black plays the automatic first move, so the player is white.
        assertTrue(LichessPuzzleCsv.parseLine(sample).playerIsWhite)
        val whiteToMove = sample.replace(" b - - 0 24", " w - - 0 24")
        assertFalse(LichessPuzzleCsv.parseLine(whiteToMove).playerIsWhite)
    }

    @Test
    fun acceptsPromotionMoves() {
        val line = sample.replace("f2g3 e6e7 b2b1 b3c1 b1c1 h6c1", "a2a1q e6e7")
        assertEquals(listOf("a2a1q", "e6e7"), LichessPuzzleCsv.parseLine(line).moves)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsBadUciMove() {
        LichessPuzzleCsv.parseLine(sample.replace("e6e7", "e6e9"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsOddNumberOfMoves() {
        LichessPuzzleCsv.parseLine(sample.replace("f2g3 e6e7 b2b1 b3c1 b1c1 h6c1", "f2g3 e6e7 b2b1"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsSingleMove() {
        LichessPuzzleCsv.parseLine(sample.replace("f2g3 e6e7 b2b1 b3c1 b1c1 h6c1", "f2g3"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsTooFewColumns() {
        LichessPuzzleCsv.parseLine("00008,fen,moves")
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsNonNumericRating() {
        LichessPuzzleCsv.parseLine(sample.replace(",1913,", ",abc,"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsShortFen() {
        LichessPuzzleCsv.parseLine(sample.replace(" b - - 0 24", " b"))
    }

    @Test
    fun parseAllSkipsHeaderAndBlankLinesAndReportsBadRows() {
        val errors = mutableListOf<Int>()
        val lines = sequenceOf(LichessPuzzleCsv.HEADER, "", sample, "garbage", sample.replace("00008", "00009"))
        val puzzles = LichessPuzzleCsv.parseAll(lines) { n, _ -> errors += n }
        assertEquals(listOf("00008", "00009"), puzzles.map { it.id })
        assertEquals(listOf(4), errors)
    }

    @Test
    fun quotedFieldsMayContainCommas() {
        val line = sample.replace("https://lichess.org/787zsVup/black#48", "\"https://lichess.org/a,b\"")
        assertEquals("https://lichess.org/a,b", LichessPuzzleCsv.parseLine(line).gameUrl)
    }
}
