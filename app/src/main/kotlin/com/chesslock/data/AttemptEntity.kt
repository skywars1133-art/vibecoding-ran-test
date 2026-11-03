package com.chesslock.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Append-only log of completed puzzle attempts (SPEC 5). It powers the local statistics screen;
 * nothing here is uploaded while accounts are disabled.
 *
 * A LOSS is written the moment the third life is lost (that is when ELO changes). If the player
 * keeps going and solves the puzzle, the same row is completed instead of adding a second attempt.
 */
@Entity(
    tableName = "attempts",
    indices = [
        Index(value = ["puzzleId"], name = "index_attempts_puzzleId"),
        Index(value = ["playedAtEpochMillis"], name = "index_attempts_playedAtEpochMillis"),
    ],
)
data class AttemptEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val puzzleId: String,
    val puzzleRating: Int,
    /** Lichess themes, space-separated, copied so statistics survive a puzzle row change. */
    val themes: String,
    val solved: Boolean,
    val skipped: Boolean,
    val hintsUsed: Int,
    val eloBefore: Int,
    val eloAfter: Int,
    val rewardSeconds: Long,
    val playedAtEpochMillis: Long,
    /** Local date (YYYY-MM-DD) used for streaks; never a UTC day. */
    val day: String,
)
