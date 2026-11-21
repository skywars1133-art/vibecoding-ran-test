package com.chesslock.di

import com.chesslock.data.FirebaseLeaderboardIdentity
import com.chesslock.data.FirebaseLeaderboardRepository
import com.chesslock.data.LeaderboardIdentity
import com.chesslock.data.LeaderboardRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** The local implementations remain in the source tree as tests/fallback; production uses Firebase. */
@Module
@InstallIn(SingletonComponent::class)
abstract class LeaderboardModule {
    @Binds
    @Singleton
    abstract fun bindLeaderboardRepository(impl: FirebaseLeaderboardRepository): LeaderboardRepository

    @Binds
    @Singleton
    abstract fun bindLeaderboardIdentity(impl: FirebaseLeaderboardIdentity): LeaderboardIdentity
}
