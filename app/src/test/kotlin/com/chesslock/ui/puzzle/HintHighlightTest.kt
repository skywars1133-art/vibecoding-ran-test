package com.chesslock.ui.puzzle

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HintHighlightTest {

    @Test
    fun noHighlightWithoutAHintLevel() {
        assertNull(HintHighlight.forMove("e2e4", 0))
        assertNull(HintHighlight.forMove(null, 1))
    }

    @Test
    fun firstLevelHighlightsOnlyThePieceToMove() {
        val hint = HintHighlight.forMove("e2e4", 1)!!
        assertEquals("e2", hint.sourceSquare)
        assertNull(hint.targetSquare)
    }

    @Test
    fun secondLevelAlsoHighlightsTheTargetSquare() {
        val hint = HintHighlight.forMove("e2e4", 2)!!
        assertEquals("e2", hint.sourceSquare)
        assertEquals("e4", hint.targetSquare)
    }

    @Test
    fun promotionMovesUseTheSameFourSquares() {
        val hint = HintHighlight.forMove("e7e8q", 2)!!
        assertEquals("e7", hint.sourceSquare)
        assertEquals("e8", hint.targetSquare)
    }

    @Test
    fun castlingMovesUseTheKingSquares() {
        val hint = HintHighlight.forMove("E1G1", 2)!!
        assertEquals("e1", hint.sourceSquare)
        assertEquals("g1", hint.targetSquare)
    }

    @Test
    fun malformedMoveIsIgnored() {
        assertNull(HintHighlight.forMove("e2", 2))
    }
}
