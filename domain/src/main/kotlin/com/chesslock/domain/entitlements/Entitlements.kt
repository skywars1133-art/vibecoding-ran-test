package com.chesslock.domain.entitlements

import com.chesslock.domain.config.AppConfig

/**
 * Features a future Play Billing "Premium" purchase can unlock (SPEC 3b, PHASES Phase 4).
 * Payments are deliberately NOT implemented yet; the app always runs on [Entitlements.FREE].
 */
enum class PremiumFeature {
    /** Hints are granted immediately, without watching a rewarded ad. */
    AD_FREE_HINTS,

    /** The daily free-skip allowance grows by [AppConfig.premiumExtraSkipsPerDay]. */
    EXTRA_SKIPS,
}

/** Immutable snapshot of the currently unlocked premium features. */
data class Entitlements(val unlocked: Set<PremiumFeature> = emptySet()) {
    fun isUnlocked(feature: PremiumFeature): Boolean = feature in unlocked

    val isPremium: Boolean get() = unlocked.isNotEmpty()

    companion object {
        /** The default, no-purchase state. */
        val FREE = Entitlements()
    }
}

/**
 * Pure rules that turn an [Entitlements] snapshot into gameplay limits, so feature code never
 * checks a purchase flag directly and Billing can change the snapshot without rewrites.
 */
object EntitlementRules {
    /** True when tapping Hint must run a rewarded ad before the hint is granted. */
    fun hintsRequireAd(entitlements: Entitlements): Boolean =
        !entitlements.isUnlocked(PremiumFeature.AD_FREE_HINTS)

    /** Daily free skips for this player, including the premium bonus when unlocked. */
    fun freeSkipsPerDay(config: AppConfig, entitlements: Entitlements): Int =
        config.freeSkipsPerDay +
            if (entitlements.isUnlocked(PremiumFeature.EXTRA_SKIPS)) {
                config.premiumExtraSkipsPerDay
            } else {
                0
            }
}
