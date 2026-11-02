package com.chesslock.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.chesslock.BuildConfig
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** What the Hint button can tell the player about the next rewarded ad. */
sealed interface RewardedAdState {
    /** The SDK is initializing or an ad is being requested. */
    data object Loading : RewardedAdState

    /** An ad is loaded and can be shown immediately. */
    data object Ready : RewardedAdState

    /** Load failed (typically offline or no fill); [message] is for logs, never for UI text. */
    data class Unavailable(val message: String? = null) : RewardedAdState
}

/**
 * Preloads and shows the single AdMob rewarded ad used for hints (SPEC 3b). Hints are granted only
 * from the ad's `onUserEarnedReward` callback; there is deliberately no free fallback.
 *
 * The next ad is requested as soon as one is shown, so the following hint is ready without waiting.
 */
@Singleton
class RewardedAdManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val _state = MutableStateFlow<RewardedAdState>(RewardedAdState.Loading)
    val state: StateFlow<RewardedAdState> = _state.asStateFlow()

    private var rewardedAd: RewardedAd? = null
    private var loading = false

    /** Call once from the Application; safe to call again (for example after process recreation). */
    fun initialize() {
        MobileAds.initialize(context) { preload() }
    }

    /** Requests the next ad when none is loaded or loading. Failures are reported through [state]. */
    fun preload() {
        if (rewardedAd != null || loading) return
        loading = true
        _state.value = RewardedAdState.Loading
        RewardedAd.load(
            context,
            BuildConfig.ADMOB_REWARDED_UNIT_ID,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    loading = false
                    rewardedAd = ad
                    _state.value = RewardedAdState.Ready
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    loading = false
                    rewardedAd = null
                    _state.value = RewardedAdState.Unavailable(error.message)
                    Log.w(TAG, "Rewarded ad failed to load: ${error.message}")
                }
            },
        )
    }

    /**
     * Shows the loaded ad. [onReward] runs only when the ad reports that the reward was earned.
     * Returns false when no ad is ready; the caller then keeps the hint ungranted.
     */
    fun show(activity: Activity, onReward: () -> Unit): Boolean {
        val ad = rewardedAd ?: run {
            preload()
            return false
        }
        rewardedAd = null
        _state.value = RewardedAdState.Loading
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                preload()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                Log.w(TAG, "Rewarded ad failed to show: ${error.message}")
                _state.value = RewardedAdState.Unavailable(error.message)
                preload()
            }
        }
        ad.show(activity) { onReward() }
        return true
    }

    private companion object {
        const val TAG = "RewardedAdManager"
    }
}
