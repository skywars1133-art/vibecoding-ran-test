package com.chesslock.data

import com.chesslock.domain.config.AppConfig
import com.chesslock.domain.leaderboard.LeaderboardRanks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/** Anonymous Auth: no sign-in UI, one ranking identity per install. */
@Singleton
class FirebaseLeaderboardIdentity @Inject constructor(
    private val config: AppConfig,
) : LeaderboardIdentity {
    private val auth: FirebaseAuth get() = FirebaseAuth.getInstance()

    override suspend fun playerId(): String = signInOnce().uid

    /** Called after [playerId] from the ViewModel, so the uid is available by then. */
    override fun nickname(): String = LeaderboardRanks.defaultNickname(auth.currentUser?.uid.orEmpty(), config)

    private suspend fun signInOnce(): FirebaseUser {
        auth.currentUser?.let { return it }
        return auth.signInAnonymously().await().user
            ?: error("Anonymous sign-in returned no user")
    }
}
