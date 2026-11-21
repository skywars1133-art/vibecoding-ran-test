package com.chesslock.data

import com.chesslock.domain.config.AppConfig
import com.chesslock.domain.leaderboard.LeaderboardEntry
import com.chesslock.domain.leaderboard.LeaderboardRanks
import com.chesslock.domain.leaderboard.LeaderboardSnapshot
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Global ranking backed by `leaderboard/{uid}`. Only nickname, ELO and rated-puzzle count are
 * written; app-usage data is never part of this repository. The collection is read in full so the
 * ranking order and "my rank" use exactly the same pure rules as the local preview (no composite
 * indexes needed; replace with a top-N query + count if the board ever grows large).
 */
@Singleton
class FirebaseLeaderboardRepository @Inject constructor(
    private val identity: LeaderboardIdentity,
    private val config: AppConfig,
) : LeaderboardRepository {
    private val collection: CollectionReference
        get() = FirebaseFirestore.getInstance().collection(LeaderboardFirestoreMapper.COLLECTION)

    override suspend fun load(myPlayerId: String): LeaderboardSnapshot {
        identity.playerId() // Firestore rules require an authenticated user.
        val entries = collection.get().await().documents.mapNotNull { document ->
            LeaderboardFirestoreMapper.toEntry(document.id, document.data, config)
        }
        return LeaderboardRanks.snapshot(entries, myPlayerId, config.leaderboardTopLimit)
    }

    override suspend fun publish(entry: LeaderboardEntry) {
        identity.playerId()
        collection.document(entry.playerId)
            .set(LeaderboardFirestoreMapper.toDocument(entry, config))
            .await()
    }
}
