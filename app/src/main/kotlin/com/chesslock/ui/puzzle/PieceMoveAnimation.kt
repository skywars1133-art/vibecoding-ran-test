package com.chesslock.ui.puzzle

import com.chesslock.chess.ChessPiece
import com.chesslock.chess.ChessPieceType

/** One piece travelling between two squares during a board animation. */
internal data class AnimatedPiece(
    val piece: ChessPiece,
    val fromSquare: String,
    val toSquare: String,
    /** Piece that occupied [toSquare] before the move; kept drawn until the mover lands. */
    val capturedPiece: ChessPiece?,
)

/** A single animation frame batch: the pieces between two displayed positions. */
internal data class PieceMoveStep(
    val animatedPieces: List<AnimatedPiece>,
    val piecesBefore: List<ChessPiece>,
    val piecesAfter: List<ChessPiece>,
)

/**
 * Turns the UCI moves emitted with one state update into animation steps. The ViewModel can play a
 * player move and the puzzle reply before publishing a single state, so a batch may contain two plies.
 */
internal fun buildPieceMoveSteps(
    piecesBefore: List<ChessPiece>,
    uciMoves: List<String>,
): List<PieceMoveStep> {
    val board = piecesBefore.associateBy { it.square.lowercase() }.toMutableMap()
    val steps = mutableListOf<PieceMoveStep>()

    uciMoves.forEach { uci ->
        val move = parseUciMove(uci) ?: return@forEach
        val sourcePiece = board[move.from] ?: return@forEach
        val before = board.toMap()

        val animatedPieces = mutableListOf<AnimatedPiece>()
        val capturedPiece = board[move.to]?.takeIf { it.isWhite != sourcePiece.isWhite }
        val movingPiece = if (move.promotion != null && sourcePiece.type == ChessPieceType.PAWN) {
            sourcePiece.copy(type = move.promotion, square = move.to)
        } else {
            sourcePiece.copy(square = move.to)
        }
        animatedPieces += AnimatedPiece(movingPiece, move.from, move.to, capturedPiece)

        castlingRookMove(board, move, sourcePiece)?.let { (rookFrom, rookTo) ->
            val rook = board.getValue(rookFrom).copy(square = rookTo)
            animatedPieces += AnimatedPiece(rook, rookFrom, rookTo, capturedPiece = null)
        }

        // Remove every source before placing destinations so two pieces cannot overwrite each other.
        animatedPieces.forEach { board.remove(it.fromSquare) }
        animatedPieces.forEach { board[it.toSquare] = it.piece }

        steps += PieceMoveStep(
            animatedPieces = animatedPieces,
            piecesBefore = before.values.toList(),
            piecesAfter = board.values.toList(),
        )
    }
    return steps
}

/** Display grid position (column, row) of a square for the current board orientation. */
internal fun squareDisplayIndex(square: String, playerIsWhite: Boolean): Pair<Int, Int>? {
    val normalized = square.trim().lowercase()
    if (normalized.length != 2) return null
    val file = normalized[0] - 'a'
    val rank = normalized[1] - '0'
    if (file !in 0..7 || rank !in 1..8) return null
    return if (playerIsWhite) {
        file to 8 - rank
    } else {
        7 - file to rank - 1
    }
}

private data class ParsedMove(
    val from: String,
    val to: String,
    val promotion: ChessPieceType?,
)

private fun parseUciMove(uci: String): ParsedMove? {
    val move = uci.trim().lowercase()
    if (move.length !in 4..5) return null
    val from = move.substring(0, 2)
    val to = move.substring(2, 4)
    if (!isSquare(from) || !isSquare(to) || from == to) return null
    val promotion = if (move.length == 5) promotionType(move[4]) ?: return null else null
    return ParsedMove(from, to, promotion)
}

private fun isSquare(square: String): Boolean =
    square.length == 2 && square[0] in 'a'..'h' && square[1] in '1'..'8'

private fun promotionType(letter: Char): ChessPieceType? = when (letter) {
    'q' -> ChessPieceType.QUEEN
    'r' -> ChessPieceType.ROOK
    'b' -> ChessPieceType.BISHOP
    'n' -> ChessPieceType.KNIGHT
    else -> null
}

private fun castlingRookMove(
    board: Map<String, ChessPiece>,
    move: ParsedMove,
    king: ChessPiece,
): Pair<String, String>? {
    if (king.type != ChessPieceType.KING) return null
    val homeRank = if (king.isWhite) '1' else '8'
    if (move.from != "e$homeRank" || move.to !in setOf("g$homeRank", "c$homeRank")) return null
    val rookFromFile = if (move.to[0] == 'g') 'h' else 'a'
    val rookToFile = if (move.to[0] == 'g') 'f' else 'd'
    val rookFrom = "$rookFromFile$homeRank"
    val rook = board[rookFrom] ?: return null
    if (rook.type != ChessPieceType.ROOK || rook.isWhite != king.isWhite) return null
    return rookFrom to "$rookToFile$homeRank"
}
