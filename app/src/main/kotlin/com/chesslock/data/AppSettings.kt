package com.chesslock.data

import com.chesslock.domain.blocking.BlockCategory
import com.chesslock.domain.config.AppConfig

/**
 * Piece sets the board can render. Text styles need no assets; [CBURNETT] uses bundled
 * VectorDrawables and the other artwork styles load licensed SVG assets from `assets/pieces`.
 */
enum class PieceStyle(val storageKey: String, val svgFolder: String? = null) {
    CLASSIC("classic"),
    OUTLINE("outline"),
    LETTERS("letters"),
    CBURNETT("cburnett"),
    CHESSNUT("chessnut", "chessnut"),
    FANTASY("fantasy", "fantasy"),
    CELTIC("celtic", "celtic"),
    SPATIAL("spatial", "spatial"),
    ;

    /** True for styles drawn with text glyphs (no artwork assets). */
    val isText: Boolean get() = this != CBURNETT && svgFolder == null

    companion object {
        /** Cburnett (BSD-3-Clause) is the default because its license clearly allows commercial use. */
        val DEFAULT = CBURNETT

        fun fromStorageKey(key: String?): PieceStyle =
            entries.firstOrNull { it.storageKey == key } ?: DEFAULT
    }
}

/** In-app language override; [SYSTEM] follows the device locale. */
enum class AppLanguage(val storageKey: String, val languageTag: String?) {
    SYSTEM("system", null),
    ENGLISH("english", "en"),
    SLOVAK("slovak", "sk"),
    ;

    companion object {
        val DEFAULT = SYSTEM

        fun fromStorageKey(key: String?): AppLanguage =
            entries.firstOrNull { it.storageKey == key } ?: DEFAULT
    }
}

/** Light/dark override; [SYSTEM] follows the device theme (SPEC 6). */
enum class ThemeMode(val storageKey: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark"),
    ;

    companion object {
        val DEFAULT = SYSTEM

        fun fromStorageKey(key: String?): ThemeMode =
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
    val language: AppLanguage = AppLanguage.DEFAULT,
    val themeMode: ThemeMode = ThemeMode.DEFAULT,
    /** SPEC 2: download puzzle packs only on an unmetered network by default. */
    val wifiOnlyDownloads: Boolean,
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
            wifiOnlyDownloads = config.wifiOnlyDownloadsByDefault,
            blockingEnabled = config.blockingEnabledByDefault,
            dailyLimitMinutes = config.defaultDailyLimitMinutes,
            dailyCapMinutes = config.dailyCapMinutes,
            blockedCategories = config.defaultBlockedCategories,
        )
    }
}
