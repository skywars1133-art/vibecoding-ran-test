package com.chesslock.ui.puzzle

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.chesslock.R
import com.chesslock.chess.ChessPiece
import com.chesslock.data.PieceStyle

/**
 * Interactive board shared by normal puzzle play and the placement test. It only knows the position
 * and which moves are legal; all rules and persistence live in the caller.
 */
@Composable
fun ChessBoard(
    pieces: List<ChessPiece>,
    legalMoves: List<String>,
    lastMove: String?,
    playerIsWhite: Boolean,
    solved: Boolean,
    onMove: (String) -> Unit,
    pieceStyle: PieceStyle = PieceStyle.DEFAULT,
    resetKey: Any? = null,
    modifier: Modifier = Modifier,
) {
    var selectedSquare by remember(resetKey, lastMove, solved) {
        mutableStateOf<String?>(null)
    }
    var promotionMoves by remember(resetKey, lastMove, solved) {
        mutableStateOf<List<String>>(emptyList())
    }
    val files = if (playerIsWhite) ('a'..'h').toList() else ('a'..'h').toList().reversed()
    val ranks = if (playerIsWhite) (8 downTo 1).toList() else (1..8).toList()

    fun selectSquare(square: String) {
        if (solved) return
        val selected = selectedSquare
        if (selected == null) {
            if (legalMoves.any { it.startsWith(square) }) selectedSquare = square
            return
        }
        val candidates = legalMoves.filter {
            it.startsWith(selected) && it.substring(2, 4) == square
        }
        when {
            candidates.size > 1 -> promotionMoves = candidates
            candidates.size == 1 -> {
                selectedSquare = null
                onMove(candidates.single())
            }
            square == selected -> selectedSquare = null
            legalMoves.any { it.startsWith(square) } -> selectedSquare = square
            else -> selectedSquare = null
        }
    }

    BoxWithConstraints(modifier = modifier.aspectRatio(1f)) {
        val pieceSize = with(LocalDensity.current) { (maxWidth / 8).toSp() * 0.78f }
        Column(Modifier.fillMaxSize()) {
            ranks.forEach { rank ->
                Row(Modifier.weight(1f)) {
                    files.forEach { file ->
                        val square = "$file$rank"
                        val piece = pieces.firstOrNull { it.square == square }
                        val isLastMove = lastMove?.let {
                            square == it.substring(0, 2) || square == it.substring(2, 4)
                        } == true
                        val isDark = (file - 'a' + rank - 1) % 2 == 0
                        val background = when {
                            square == selectedSquare -> Color(0xFF80C783)
                            isLastMove -> if (isDark) Color(0xFFBCAA4A) else Color(0xFFF5DF6C)
                            isDark -> Color(0xFFB58863)
                            else -> Color(0xFFF0D9B5)
                        }
                        val selected = selectedSquare
                        val isTarget = selected != null && legalMoves.any {
                            it.startsWith(selected) && it.substring(2, 4) == square
                        }
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(background)
                                .clickable { selectSquare(square) },
                        ) {
                            if (isTarget && piece == null) {
                                Box(Modifier.size(14.dp).background(Color(0x66000000), CircleShape))
                            }
                            if (piece != null) {
                                PieceGlyph(
                                    type = piece.type,
                                    isWhite = piece.isWhite,
                                    style = pieceStyle,
                                    fontSize = pieceSize,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (promotionMoves.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { promotionMoves = emptyList() },
            title = { Text(stringResource(R.string.promotion_title)) },
            text = { Text(stringResource(R.string.promotion_message)) },
            confirmButton = {
                Row {
                    promotionMoves.sorted().forEach { move ->
                        TextButton(onClick = {
                            promotionMoves = emptyList()
                            selectedSquare = null
                            onMove(move)
                        }) {
                            Text(promotionLabel(move.last()))
                        }
                    }
                }
            },
        )
    }
}

@Composable
private fun promotionLabel(piece: Char): String = stringResource(
    when (piece) {
        'q' -> R.string.promotion_queen
        'r' -> R.string.promotion_rook
        'b' -> R.string.promotion_bishop
        else -> R.string.promotion_knight
    },
)
