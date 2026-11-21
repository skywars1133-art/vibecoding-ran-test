package com.chesslock.audio

import com.chesslock.ui.puzzle.PuzzleMoveStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SoundCueTest {

    @Test
    fun `move status maps to the matching feedback sound`() {
        assertEquals(SoundCue.MOVE, SoundCues.forMove(PuzzleMoveStatus.CORRECT))
        assertEquals(SoundCue.WRONG, SoundCues.forMove(PuzzleMoveStatus.WRONG))
        assertEquals(SoundCue.SOLVED, SoundCues.forMove(PuzzleMoveStatus.SOLVED))
        assertNull(SoundCues.forMove(PuzzleMoveStatus.NONE))
    }
}
