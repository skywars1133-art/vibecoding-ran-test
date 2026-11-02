package com.chesslock.domain.puzzle

/**
 * Asks the chess rules layer whether a move delivers checkmate. Implemented later on top of the chess
 * library; kept as an interface so this module stays free of chess rules and fully unit-testable.
 */
fun interface MateChecker {
    /**
     * @param movesSoFar every UCI move played from the puzzle FEN so far (including the opponent's first move)
     * @param candidate the player's move (UCI), already known to be different from the expected solution move
     */
    fun deliversMate(movesSoFar: List<String>, candidate: String): Boolean
}

sealed interface MoveResult {
    /** The move was wrong; the position is unchanged. The caller reports it to [PuzzleSession.wrongMove]. */
    data object Wrong : MoveResult

    /**
     * The move was accepted. [reply] is the opponent's answer to play automatically (null if none),
     * [solved] is true when the puzzle is complete, [alternativeMate] when a different mating move was accepted.
     */
    data class Correct(
        val reply: String?,
        val solved: Boolean,
        val alternativeMate: Boolean,
    ) : MoveResult
}

/**
 * Walks through the solution of one [Puzzle]: plays the opponent's moves automatically and checks the
 * player's moves. Accepts any move that delivers mate, as Lichess does.
 */
class PuzzleProgress(
    val puzzle: Puzzle,
    private val mateChecker: MateChecker,
) {
    private val played = mutableListOf<String>()
    private var index = 0
    private var started = false

    var solved: Boolean = false
        private set

    /** UCI moves played so far from the puzzle FEN (opponent and player). */
    val movesPlayed: List<String> get() = played.toList()

    /** The move the player has to find next, or null when solved. Used for hints (never shown otherwise). */
    val expectedMove: String?
        get() = if (!started || solved) null else puzzle.moves[index]

    /** Plays the opponent's opening move and returns it so the board can animate it. Call once. */
    fun start(): String {
        check(!started) { "Already started" }
        started = true
        val first = puzzle.moves[0]
        played += first
        index = 1
        return first
    }

    /** [uci] is the player's move; for promotions it carries the piece letter (e.g. "e7e8q"). */
    fun submit(uci: String): MoveResult {
        check(started) { "Call start() first" }
        check(!solved) { "Puzzle already solved" }
        val move = uci.trim().lowercase()
        val expected = puzzle.moves[index]

        if (move == expected) {
            played += move
            index += 1
            return advance(alternativeMate = false)
        }
        if (mateChecker.deliversMate(played.toList(), move)) {
            played += move
            solved = true
            return MoveResult.Correct(reply = null, solved = true, alternativeMate = true)
        }
        return MoveResult.Wrong
    }

    private fun advance(alternativeMate: Boolean): MoveResult.Correct {
        if (index >= puzzle.moves.size) {
            solved = true
            return MoveResult.Correct(reply = null, solved = true, alternativeMate = alternativeMate)
        }
        val reply = puzzle.moves[index]
        played += reply
        index += 1
        return MoveResult.Correct(reply = reply, solved = false, alternativeMate = alternativeMate)
    }
}
