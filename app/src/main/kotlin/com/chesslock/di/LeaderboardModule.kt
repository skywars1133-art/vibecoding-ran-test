package com.chesslock.di

import com.chesslock.data.LeaderboardIdentity
import com.chesslock.data.LeaderboardRepository
import com.chesslock.data.LocalLeaderboardIdentity
import com.chesslock.data.LocalLeaderboardRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Swap these bindings for the Firebase implementations once the project config is present. */
@Module
@InstallIn(SingletonComponent::class)
abstract class LeaderboardModule {
    @Binds
    @Singleton
    abstract fun bindLeaderboardRepository(impl: LocalLeaderboardRepository): LeaderboardRepository

    @Binds
    @Singleton
    abstract fun bindLeaderboardIdentity(impl: LocalLeaderboardIdentity): LeaderboardIdentity
}
