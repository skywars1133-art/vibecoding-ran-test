package com.chesslock.di

import android.content.Context
import androidx.room.Room
import com.chesslock.data.ChessLockDatabase
import com.chesslock.data.MIGRATION_1_2
import com.chesslock.data.PuzzleDao
import com.chesslock.data.StatsDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object StorageModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ChessLockDatabase =
        Room.databaseBuilder(context, ChessLockDatabase::class.java, "chesslock.db")
            .addMigrations(MIGRATION_1_2)
            .build()

    @Provides
    fun providePuzzleDao(database: ChessLockDatabase): PuzzleDao = database.puzzleDao()

    @Provides
    fun provideStatsDao(database: ChessLockDatabase): StatsDao = database.statsDao()
}
