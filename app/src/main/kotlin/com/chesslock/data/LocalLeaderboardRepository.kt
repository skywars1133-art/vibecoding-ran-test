package com.chesslock.data

import com.chesslock.domain.config.AppConfig
import com.chesslock.domain.leaderboard.LeaderboardEntry
import com.chesslock.domain.leaderboard.LeaderboardRanks
import com.chesslock.domain.leaderboard.LeaderboardSnapshot
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Config-independent preview source. It never talks to a server: a few clearly fixed rows plus
 * the one entry published by this install. Keeps builds and the UI testable before Firebase is
 * configured, and is replaced by the Firestore implementation once `google-services.json` exists.
 */
@Singleton
class LocalLeaderboardRepository @Inject constructor(
    private val config: AppConfig,
) : LeaderboardRepository {
    private val published = mutableMapOf<String, LeaderboardEntry>()

    override suspend fun load(myPlayerId: String): LeaderboardSnapshot =
        LeaderboardRanks.snapshot(SAMPLE_ENTRIES + published.values, myPlayerId, config.leaderboardTopLimit)

    override suspend fun publish(entry: LeaderboardEntry) {
        val safeEntry = entry.copy(
            nickname = LeaderboardRanks.normalizeNickname(entry.nickname, config),
            elo = entry.elo.coerceIn(config.eloFloor, config.placementMaxRating),
            ratedPuzzlesPlayed = entry.ratedPuzzlesPlayed.coerceAtLeast(0),
        )
        published[safeEntry.playerId] = safeEntry
    }

    companion object {
        const val LOCAL_PLAYER_ID = "local-install"

        /** Placeholder rows so the ranking UI can be exercised without a cloud project. */
        private val SAMPLE_ENTRIES = listOf(
            LeaderboardEntry("sample-1", "Ada", 2180, 142),
            LeaderboardEntry("sample-2", "Veľký majster", 2040, 98),
            LeaderboardEntry("sample-3", "Katka", 1875, 61),
        )
    }
}

/** Local identity used until Anonymous Auth is wired up. */
@Singleton
class LocalLeaderboardIdentity @Inject constructor(
    private val config: AppConfig,
) : LeaderboardIdentity {
    override suspend fun playerId(): String = LocalLeaderboardRepository.LOCAL_PLAYER_ID

    override fun nickname(): String = config.leaderboardDefaultNickname
}
