package com.chesslock.ui.puzzle

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.chesslock.R
import com.chesslock.chess.ChessPiece
import com.chesslock.data.PieceStyle
import com.chesslock.domain.config.AppConfig
import kotlin.math.roundToInt

/**
 * Interactive board shared by normal puzzle play and the placement test. It only knows the position
 * and which moves are legal; all rules and persistence live in the caller. [moveTimeline] carries the
 * UCI plies behind the latest state update so the player's move and the puzzle reply can glide in turn.
 */
@Composable
fun ChessBoard(
    pieces: List<ChessPiece>,
    legalMoves: List<String>,
    lastMove: String?,
    playerIsWhite: Boolean,
    solved: Boolean,
    onMove: (String) -> Unit,
    modifier: Modifier = Modifier,
    pieceStyle: PieceStyle = PieceStyle.DEFAULT,
    hintSourceSquare: String? = null,
    hintTargetSquare: String? = null,
    resetKey: Any? = null,
    moveTimeline: List<String> = emptyList(),
    animationDurationMs: Int = AppConfig.DEFAULT.boardMoveAnimationDurationMs,
) {
    var selectedSquare by remember(resetKey, lastMove, solved) {
        mutableStateOf<String?>(null)
    }
    var promotionMoves by remember(resetKey, lastMove, solved) {
        mutableStateOf<List<String>>(emptyList())
    }
    var displayedPieces by remember(resetKey) { mutableStateOf(pieces) }
    var animationSteps by remember(resetKey) {
        mutableStateOf<List<PieceMoveStep>>(emptyList())
    }
    val moveProgress = remember(resetKey) { Animatable(1f) }

    LaunchedEffect(resetKey, pieces, moveTimeline) {
        if (moveTimeline.isEmpty()) {
            animationSteps = emptyList()
            moveProgress.snapTo(1f)
            displayedPieces = pieces
            return@LaunchedEffect
        }
        val steps = buildPieceMoveSteps(displayedPieces, moveTimeline)
        if (steps.isEmpty()) {
            animationSteps = emptyList()
            moveProgress.snapTo(1f)
            displayedPieces = pieces
            return@LaunchedEffect
        }
        animationSteps = steps
        for (step in steps) {
            moveProgress.snapTo(0f)
            moveProgress.animateTo(1f, tween(animationDurationMs, easing = FastOutSlowInEasing))
            displayedPieces = step.piecesAfter
            animationSteps = animationSteps.drop(1)
        }
        animationSteps = emptyList()
        displayedPieces = pieces
    }

    val files = if (playerIsWhite) ('a'..'h').toList() else ('a'..'h').toList().reversed()
    val ranks = if (playerIsWhite) (8 downTo 1).toList() else (1..8).toList()

    fun selectSquare(square: String) {
        if (solved || animationSteps.isNotEmpty()) return
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
        val cell = maxWidth / 8
        val cellPx = with(LocalDensity.current) { cell.toPx() }
        val pieceSize = with(LocalDensity.current) { cell.toSp() * 0.78f }
        val activeStep = animationSteps.firstOrNull()
        val animatedSources = activeStep?.animatedPieces?.map { it.fromSquare }?.toSet().orEmpty()
        val capturedPieces = activeStep?.animatedPieces?.mapNotNull { animated ->
            animated.capturedPiece?.let { animated.toSquare to it }
        }?.toMap().orEmpty()
        val visiblePieces = if (activeStep == null) {
            displayedPieces
        } else {
            displayedPieces
                .filterNot { it.square in animatedSources }
                .map { capturedPieces[it.square] ?: it }
        }

        Column(Modifier.fillMaxSize()) {
            ranks.forEach { rank ->
                Row(Modifier.weight(1f)) {
                    files.forEach { file ->
                        val square = "$file$rank"
                        val piece = visiblePieces.firstOrNull { it.square == square }
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
                        val hintColor = when {
                            square == hintTargetSquare -> Color(0xFF1565C0)
                            square == hintSourceSquare -> Color(0xFF2196F3)
                            else -> null
                        }
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(background)
                                .then(
                                    if (hintColor != null) {
                                        Modifier.border(3.dp, hintColor)
                                    } else {
                                        Modifier
                                    },
                                )
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

        // Moving pieces live above the squares: the old square is already empty and a captured piece
        // stays under the mover until the glide finishes.
        Box(Modifier.fillMaxSize()) {
            activeStep?.animatedPieces?.forEach { animated ->
                val from = squareDisplayIndex(animated.fromSquare, playerIsWhite) ?: return@forEach
                val to = squareDisplayIndex(animated.toSquare, playerIsWhite) ?: return@forEach
                val fraction = moveProgress.value
                val x = (from.first + (to.first - from.first) * fraction) * cellPx
                val y = (from.second + (to.second - from.second) * fraction) * cellPx
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .offset { IntOffset(x.roundToInt(), y.roundToInt()) }
                        .size(cell),
                ) {
                    PieceGlyph(
                        type = animated.piece.type,
                        isWhite = animated.piece.isWhite,
                        style = pieceStyle,
                        fontSize = pieceSize,
                        modifier = Modifier.fillMaxSize(),
                    )
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
