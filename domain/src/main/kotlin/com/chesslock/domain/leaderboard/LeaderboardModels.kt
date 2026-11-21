package com.chesslock.domain.leaderboard

/**
 * The only chess data a leaderboard client ever sees: a stable player id, a display nickname and
 * the two local statistics that decide the rank. No app-usage data, attempt history or settings
 * are part of this model (SPEC 5 privacy constraint).
 */
data class LeaderboardEntry(
    val playerId: String,
    val nickname: String,
    val elo: Int,
    val ratedPuzzlesPlayed: Int,
)

/** One row of a ranked board. Ranks start at 1. */
data class RankedEntry(
    val rank: Int,
    val entry: LeaderboardEntry,
)

/**
 * An immutable view of the board: the first [top] places plus the current player's row even when
 * it is outside the top (so "my rank" keeps working for every player).
 */
data class LeaderboardSnapshot(
    val top: List<RankedEntry>,
    val myRank: RankedEntry?,
    val totalPlayers: Int,
)
