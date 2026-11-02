package com.chesslock.ads

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Lets the Compose puzzle screen observe and show the rewarded ad without holding an Activity in a ViewModel. */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface AdsEntryPoint {
    fun rewardedAdManager(): RewardedAdManager
}
