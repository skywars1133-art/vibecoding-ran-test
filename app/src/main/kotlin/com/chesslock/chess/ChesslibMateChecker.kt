package com.chesslock.chess

import com.chesslock.domain.puzzle.MateChecker

/** Replays the attempt from its original FEN, then accepts a legal candidate only if it delivers mate. */
class ChesslibMateChecker(private val initialFen: String) : MateChecker {
    override fun deliversMate(movesSoFar: List<String>, candidate: String): Boolean = try {
        val position = ChessPosition(initialFen)
        val validHistory = movesSoFar.all { position.play(it) }
        validHistory && position.play(candidate) && position.isMated
    } catch (_: RuntimeException) {
        false
    }
}
