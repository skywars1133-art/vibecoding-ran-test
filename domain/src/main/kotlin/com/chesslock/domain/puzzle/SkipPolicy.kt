package com.chesslock.domain.puzzle

import com.chesslock.domain.config.AppConfig

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
    ): SkipKind? = when {
        elapsedOnPuzzleSeconds >= config.skipUnlockAfterMinutes * 60L -> SkipKind.TIMEOUT
        freeSkipsUsedToday < config.freeSkipsPerDay -> SkipKind.FREE
        else -> null
    }
}
