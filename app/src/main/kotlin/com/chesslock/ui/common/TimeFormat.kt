package com.chesslock.ui.common

import java.util.Locale

/**
 * Formats a duration as `m:ss`, or `h:mm:ss` once it reaches an hour. Negative values are
 * clamped to zero so a budget can never render as "-1:-5". Shared by the puzzle, statistics
 * and home screens so the same number always looks the same.
 */
fun formatClock(seconds: Long): String {
    val total = seconds.coerceAtLeast(0L)
    val hours = total / 3600
    val minutes = (total % 3600) / 60
    val remaining = total % 60
    return if (hours > 0) {
        String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, remaining)
    } else {
        String.format(Locale.getDefault(), "%d:%02d", minutes, remaining)
    }
}
