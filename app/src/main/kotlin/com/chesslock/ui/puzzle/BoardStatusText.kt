package com.chesslock.ui.puzzle

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.chesslock.R

/** Shared one-line feedback under the board (normal play and the placement test). */
@Composable
internal fun MoveStatusText(status: PuzzleMoveStatus) {
    val message = when (status) {
        PuzzleMoveStatus.NONE -> return
        PuzzleMoveStatus.WRONG -> R.string.wrong_move
        PuzzleMoveStatus.CORRECT -> R.string.correct_move
        PuzzleMoveStatus.SOLVED -> R.string.puzzle_solved
    }
    Text(
        text = stringResource(message),
        color = if (status == PuzzleMoveStatus.WRONG) {
            MaterialTheme.colorScheme.error
        } else {
            MaterialTheme.colorScheme.primary
        },
        style = MaterialTheme.typography.titleMedium,
    )
}
