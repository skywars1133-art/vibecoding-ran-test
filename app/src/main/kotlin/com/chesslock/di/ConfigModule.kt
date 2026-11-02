package com.chesslock.di

import com.chesslock.domain.config.AppConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Single injection point for the one config file (SPEC/CLAUDE.md: no constants elsewhere). */
@Module
@InstallIn(SingletonComponent::class)
object ConfigModule {
    @Provides
    @Singleton
    fun provideAppConfig(): AppConfig = AppConfig.DEFAULT
}
