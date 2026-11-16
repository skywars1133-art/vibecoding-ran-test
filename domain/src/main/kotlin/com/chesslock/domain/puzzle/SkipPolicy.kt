package com.chesslock.domain.puzzle

import com.chesslock.domain.config.AppConfig
import com.chesslock.domain.entitlements.EntitlementRules
import com.chesslock.domain.entitlements.Entitlements

enum class SkipKind {
    /** Uses up one of the daily free skips. */
    FREE,

    /** Allowed because the player has been on this puzzle long enough; costs no free skip. */
    TIMEOUT,
}

/** Skip rules (SPEC 3). A skip gives no reward and no ELO change. */
object SkipPolicy {

    /** Returns which kind of skip is available right now, or `null` if none. */
    fun availableSkip(
        freeSkipsUsedToday: Int,
        elapsedOnPuzzleSeconds: Long,
        config: AppConfig = AppConfig.DEFAULT,
        entitlements: Entitlements = Entitlements.FREE,
    ): SkipKind? = when {
        elapsedOnPuzzleSeconds >= config.skipUnlockAfterMinutes * 60L -> SkipKind.TIMEOUT
        freeSkipsUsedToday < EntitlementRules.freeSkipsPerDay(config, entitlements) -> SkipKind.FREE
        else -> null
    }
}
