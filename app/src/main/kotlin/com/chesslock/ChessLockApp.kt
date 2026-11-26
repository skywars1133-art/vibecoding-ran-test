package com.chesslock

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.chesslock.ads.RewardedAdManager
import com.chesslock.ui.common.AppPresentationController
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class ChessLockApp : Application(), Configuration.Provider {
    @Inject lateinit var rewardedAdManager: RewardedAdManager
    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var presentationController: AppPresentationController

    private val startupScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** On-demand WorkManager initialisation for `PuzzlePackWorker` (default initializer removed). */
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        // AdMob test ads: hint ads are preloaded before the first puzzle (SPEC 3b).
        rewardedAdManager.initialize()
        // Restore the saved language for system-resolved labels (launcher shortcuts, widget picker).
        startupScope.launch {
            presentationController.syncFromSettings()
        }
    }
}
