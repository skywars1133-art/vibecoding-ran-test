package com.chesslock.data

import com.chesslock.domain.blocking.BlockCategory
import com.chesslock.domain.config.AppConfig

/** Piece sets the board can render. Text styles need no assets; [CBURNETT] uses bundled artwork. */
enum class PieceStyle(val storageKey: String) {
    CLASSIC("classic"),
    OUTLINE("outline"),
    LETTERS("letters"),
    CBURNETT("cburnett"),
    ;

    companion object {
        val DEFAULT = CLASSIC

        fun fromStorageKey(key: String?): PieceStyle =
            entries.firstOrNull { it.storageKey == key } ?: DEFAULT
    }
}

/** Per-app overrides from the apps picker. */
enum class AppBlockOverride {
    DEFAULT,
    ALWAYS_BLOCK,
    NEVER_BLOCK,
}

data class AppSettings(
    val pieceStyle: PieceStyle = PieceStyle.DEFAULT,
    val blockingEnabled: Boolean,
    val dailyLimitMinutes: Int,
    val dailyCapMinutes: Int?,
    val blockedCategories: Set<BlockCategory>,
    val alwaysBlockedPackages: Set<String> = emptySet(),
    val neverBlockedPackages: Set<String> = emptySet(),
    /** The permissions onboarding is shown once; after that the status card covers missing grants. */
    val permissionsOnboardingSeen: Boolean = false,
) {
    val dailyLimitSeconds: Long get() = dailyLimitMinutes * 60L
    val dailyCapSeconds: Long? get() = dailyCapMinutes?.let { it * 60L }

    fun overrideFor(packageName: String): AppBlockOverride = when (packageName) {
        in alwaysBlockedPackages -> AppBlockOverride.ALWAYS_BLOCK
        in neverBlockedPackages -> AppBlockOverride.NEVER_BLOCK
        else -> AppBlockOverride.DEFAULT
    }

    companion object {
        fun defaults(config: AppConfig): AppSettings = AppSettings(
            blockingEnabled = config.blockingEnabledByDefault,
            dailyLimitMinutes = config.defaultDailyLimitMinutes,
            dailyCapMinutes = config.dailyCapMinutes,
            blockedCategories = config.defaultBlockedCategories,
        )
    }
}
