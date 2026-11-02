package com.chesslock.chess

import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.Piece
import com.github.bhlangonijr.chesslib.PieceType
import com.github.bhlangonijr.chesslib.Side
import com.github.bhlangonijr.chesslib.Square
import com.github.bhlangonijr.chesslib.move.Move

data class ChessPiece(
    val square: String,
    val symbol: String,
    val isWhite: Boolean,
)

/** Thread-confined chess position used by a puzzle ViewModel. */
class ChessPosition(fen: String) {
    private val board = Board().apply { loadFromFen(fen) }

    val pieces: List<ChessPiece>
        get() = Square.values().mapNotNull { square ->
            if (square == Square.NONE) return@mapNotNull null
            val piece = board.getPiece(square)
            if (piece == Piece.NONE) return@mapNotNull null
            ChessPiece(
                square = square.toString().lowercase(),
                symbol = piece.solidSymbol(),
                isWhite = piece.pieceSide == Side.WHITE,
            )
        }

    val legalMoves: List<String>
        get() = board.legalMoves().map { it.toString().lowercase() }

    val isMated: Boolean get() = board.isMated

    /** Plays only a fully legal UCI move; an invalid move leaves the position unchanged. */
    fun play(uci: String): Boolean {
        val move = findLegalMove(uci) ?: return false
        return board.doMove(move, true)
    }

    private fun findLegalMove(uci: String): Move? =
        board.legalMoves().firstOrNull { it.toString().equals(uci.trim(), ignoreCase = true) }
}

private fun Piece.solidSymbol(): String = when (pieceType) {
    PieceType.KING -> "♚"
    PieceType.QUEEN -> "♛"
    PieceType.ROOK -> "♜"
    PieceType.BISHOP -> "♝"
    PieceType.KNIGHT -> "♞"
    PieceType.PAWN -> "♟"
    else -> ""
}
