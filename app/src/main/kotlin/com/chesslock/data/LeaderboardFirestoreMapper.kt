package com.chesslock.data

import com.chesslock.domain.config.AppConfig
import com.chesslock.domain.leaderboard.LeaderboardEntry
import com.chesslock.domain.leaderboard.LeaderboardRanks

/**
 * Translation between Firestore documents and the pure leaderboard model. Kept separate from the
 * repository so the shape/validation can be unit-tested without Firebase or a device.
 */
object LeaderboardFirestoreMapper {
    const val COLLECTION = "leaderboard"
    const val FIELD_NICKNAME = "nickname"
    const val FIELD_ELO = "elo"
    const val FIELD_RATED = "ratedPuzzlesPlayed"

    fun toDocument(entry: LeaderboardEntry, config: AppConfig): Map<String, Any> = mapOf(
        FIELD_NICKNAME to LeaderboardRanks.normalizeNickname(entry.nickname, config),
        FIELD_ELO to entry.elo.coerceIn(config.eloFloor, config.placementMaxRating),
        FIELD_RATED to entry.ratedPuzzlesPlayed.coerceAtLeast(0),
    )

    /** A malformed document is ignored instead of corrupting the board. Firestore reads Longs. */
    fun toEntry(id: String, data: Map<String, Any?>?, config: AppConfig): LeaderboardEntry? {
        val values = data ?: return null
        val nickname = values[FIELD_NICKNAME] as? String ?: return null
        val elo = (values[FIELD_ELO] as? Number)?.toLong()?.toInt() ?: return null
        val played = (values[FIELD_RATED] as? Number)?.toLong()?.toInt() ?: return null
        if (!LeaderboardRanks.isValidNickname(nickname, config)) return null
        if (elo !in config.eloFloor..config.placementMaxRating || played < 0) return null
        return LeaderboardEntry(id, nickname, elo, played)
    }
}
