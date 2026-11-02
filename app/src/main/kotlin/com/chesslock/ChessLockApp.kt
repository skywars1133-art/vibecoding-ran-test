package com.chesslock

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.chesslock.ads.RewardedAdManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class ChessLockApp : Application(), Configuration.Provider {
    @Inject lateinit var rewardedAdManager: RewardedAdManager
    @Inject lateinit var workerFactory: HiltWorkerFactory

    /** On-demand WorkManager initialisation for `PuzzlePackWorker` (default initializer removed). */
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        // AdMob test ads: hint ads are preloaded before the first puzzle (SPEC 3b).
        rewardedAdManager.initialize()
    }
}
