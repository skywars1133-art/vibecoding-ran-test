package com.chesslock.ui.puzzle

/**
 * Board squares to highlight for a hint (SPEC 3b): level 1 highlights the piece to move, level 2
 * also highlights the target square. The expected move comes from the solution, never from the player.
 */
data class HintHighlight(
    val sourceSquare: String,
    val targetSquare: String?,
) {
    companion object {
        fun forMove(expectedMove: String?, level: Int): HintHighlight? {
            if (expectedMove == null || level <= 0) return null
            val move = expectedMove.trim().lowercase()
            if (move.length < 4) return null
            return HintHighlight(
                sourceSquare = move.substring(0, 2),
                targetSquare = if (level >= 2) move.substring(2, 4) else null,
            )
        }
    }
}
