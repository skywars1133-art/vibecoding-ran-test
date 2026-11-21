package com.chesslock.data

import com.chesslock.domain.leaderboard.LeaderboardEntry
import com.chesslock.domain.leaderboard.LeaderboardSnapshot

/**
 * Seam between the ranking UI and its source. The local implementation is a preview only; the
 * Firebase implementation will use Anonymous Auth + Firestore. Implementations must only ever
 * handle the chess fields in [LeaderboardEntry] — never app-usage data or settings.
 */
interface LeaderboardRepository {
    suspend fun load(myPlayerId: String): LeaderboardSnapshot

    /** Publishes (or refreshes) this install's chess stats. */
    suspend fun publish(entry: LeaderboardEntry)
}

/** Stable identity for a ranking entry. Anonymous Auth can replace the local implementation. */
interface LeaderboardIdentity {
    suspend fun playerId(): String

    fun nickname(): String
}
