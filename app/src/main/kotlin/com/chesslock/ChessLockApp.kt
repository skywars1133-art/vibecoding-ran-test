package com.chesslock

import android.app.Application
import com.chesslock.ads.RewardedAdManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class ChessLockApp : Application() {
    @Inject lateinit var rewardedAdManager: RewardedAdManager

    override fun onCreate() {
        super.onCreate()
        // AdMob test ads: hint ads are preloaded before the first puzzle (SPEC 3b).
        rewardedAdManager.initialize()
    }
}
