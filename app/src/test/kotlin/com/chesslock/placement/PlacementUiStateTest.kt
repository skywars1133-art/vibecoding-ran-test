package com.chesslock.placement

import com.chesslock.ui.placement.PlacementUiState
import org.junit.Assert.assertEquals
import org.junit.Test

class PlacementUiStateTest {

    @Test
    fun currentPuzzleNumberStartsAtOneAndStopsAtTotal() {
        val state = PlacementUiState(answered = 0, total = 10)
        assertEquals(1, state.currentPuzzleNumber)
        assertEquals(10, state.copy(answered = 9).currentPuzzleNumber)
        assertEquals(10, state.copy(answered = 12).currentPuzzleNumber)
    }

    @Test
    fun completionPercentTracksAnsweredPuzzlesAndStaysInRange() {
        val state = PlacementUiState(total = 10)
        assertEquals(0, state.copy(answered = 0).completionPercent)
        assertEquals(30, state.copy(answered = 3).completionPercent)
        assertEquals(100, state.copy(answered = 10).completionPercent)
        assertEquals(100, state.copy(answered = 12).completionPercent)
    }

    @Test
    fun missingTotalReportsNoProgress() {
        val state = PlacementUiState()
        assertEquals(0, state.currentPuzzleNumber)
        assertEquals(0, state.completionPercent)
    }
}
