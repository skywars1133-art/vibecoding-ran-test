package com.chesslock.domain.config

import com.chesslock.domain.blocking.BlockCategory

/** Extra minutes granted for puzzles rated at least [minRating]. */
data class DifficultyBonus(val minRating: Int, val bonusMinutes: Int)

/**
 * The ONE place for numeric constants (rewards, K-factor, limits, durations, penalties).
 * Nothing else in the code base may hardcode these values; read them from here.
 */
data class AppConfig(
    // --- Time budget (SPEC 1) ---
    val defaultDailyLimitMinutes: Int = 30,
    /** Optional cap on limit + earned time per day. `null` = off (default). */
    val dailyCapMinutes: Int? = null,
    /** Remaining-time thresholds (seconds) at which the user is warned: 5 min, 1 min. */
    val warnAtSeconds: List<Long> = listOf(300L, 60L),

    // --- Blocking (SPEC 1) ---
    val blockingEnabledByDefault: Boolean = true,
    /** Social, games and video are blocked until the user changes the categories. */
    val defaultBlockedCategories: Set<BlockCategory> = setOf(
        BlockCategory.SOCIAL,
        BlockCategory.GAMES,
        BlockCategory.VIDEO,
    ),
    /** Foreground app polling while the blocking service runs. */
    val blockingPollIntervalSeconds: Long = 2,
    /** Usage lookback window for [`UsageStatsManager.queryEvents`]. */
    val blockingUsageLookbackSeconds: Long = 60,
    /**
     * Upper bound for one usage step. After a process freeze or a slow poll, a huge elapsed time
     * must not drain the whole budget at once.
     */
    val blockingMaxCatchUpSeconds: Long = 10,
    /** Pending usage is written to storage this often instead of on every poll. */
    val budgetFlushIntervalSeconds: Long = 15,
    /** Daily limit choices offered in settings. */
    val dailyLimitOptionsMinutes: List<Int> = listOf(15, 30, 45, 60, 90, 120),
    /** Optional total-time cap choices offered in settings. */
    val dailyCapOptionsMinutes: List<Int> = listOf(60, 120, 180, 240),
    val blockingNotificationId: Int = 1001,
    val blockingWarningNotificationId: Int = 1002,

    // --- Rewards (SPEC 3) ---
    val baseRewardMinutes: Int = 3,
    val difficultyBonuses: List<DifficultyBonus> = listOf(
        DifficultyBonus(minRating = 0, bonusMinutes = 0),
        DifficultyBonus(minRating = 1000, bonusMinutes = 1),
        DifficultyBonus(minRating = 1400, bonusMinutes = 2),
        DifficultyBonus(minRating = 1800, bonusMinutes = 3),
        DifficultyBonus(minRating = 2200, bonusMinutes = 4),
    ),

    // --- Lives and penalties (SPEC 3) ---
    val livesPerPuzzle: Int = 3,
    /** Reward lost per wrong move once all lives are gone. */
    val penaltySecondsPerWrongMoveAfterLoss: Int = 20,

    // --- Skips (SPEC 3) ---
    val freeSkipsPerDay: Int = 1,
    /** After this long on one puzzle the player may skip regardless of free skips. */
    val skipUnlockAfterMinutes: Int = 20,
    /** UI tick used by the solving timer and time-based skip availability. */
    val puzzleTimerTickSeconds: Long = 1,

    // --- Hints (SPEC 3b) ---
    /** Level 1 highlights the piece, level 2 also highlights the target square. */
    val hintLevelsPerMove: Int = 2,
    /** Reward multiplier when at least one hint was used (0.5 = reduced by 50%). */
    val hintRewardMultiplier: Double = 0.5,
    /** Daily hint limit. `null` = unlimited (default). */
    val dailyHintLimit: Int? = null,

    // --- ELO (SPEC 4) ---
    val eloStart: Int = 1000,
    val eloFloor: Int = 400,
    val eloScale: Double = 400.0,
    val kFactorEarly: Int = 40,
    val kFactorLater: Int = 20,
    /** Number of rated puzzles that use [kFactorEarly]. */
    val kFactorEarlyPuzzleCount: Int = 20,
    val placementPuzzleCount: Int = 10,

    // --- Placement test: adaptive staircase (SPEC 4) ---
    /** First step up/down in puzzle rating; shrinks by [placementStepDecay] per puzzle. */
    val placementStepInitial: Int = 300,
    val placementStepMin: Int = 75,
    val placementStepDecay: Double = 0.75,
    val placementMaxRating: Int = 2800,

    // --- Adaptive puzzle selection (SPEC 2) ---
    /** Standard deviation of the random offset applied to the player's ELO ("mild variation"). */
    val selectionSigma: Int = 100,
    /** Pick randomly among this many unsolved puzzles closest to the desired rating. */
    val selectionPoolSize: Int = 3,

    // --- Local statistics (SPEC 5) ---
    /** Points kept for the "ELO over time" chart (plus one synthetic starting point). */
    val statsEloHistoryLimit: Int = 30,
    /** Themes shown in "Success by theme"; all other themes remain in the database. */
    val statsTopThemes: Int = 6,
    /** "This week" in the blocked-time card, including today. */
    val statsWeekDays: Int = 7,
    /**
     * Lichess tags that describe length, player type or time control rather than a puzzle motif;
     * they would drown out useful themes such as "fork", "endgame" or "mateIn2".
     */
    val statsIgnoredThemes: Set<String> = setOf(
        "short",
        "long",
        "veryLong",
        "oneMove",
        "master",
        "masterVsMaster",
        "superGM",
        "playerVsPlayer",
        "computer",
        "correspondence",
        "classical",
        "rapid",
        "blitz",
        "bullet",
        "ultraBullet",
        "chess960",
        "fromPosition",
    ),

    // --- Daily puzzle packs (SPEC 2, Phase 2) ---
    /** WorkManager asks for a fresh pack this often. */
    val puzzlePackRefreshHours: Long = 24,
    /** SPEC 2: each daily download contains this many puzzles. */
    val puzzlePackSize: Int = 150,
    /** Rating-band centers are spaced this far apart (400, 600, ... 2800). */
    val puzzlePackBandWidth: Int = 200,
    /** A pack covers the nearest band center +/- this many rating points. */
    val puzzlePackSpread: Int = 300,
    val puzzlePackMinRating: Int = 400,
    val puzzlePackMaxRating: Int = 2800,
    /**
     * Fixed GitHub Release that hosts the generated CSV packs. The pipeline is
     * `.github/workflows/puzzle-packs.yml`; the app appends `puzzles-<center>.csv`.
     */
    val puzzlePackBaseUrl: String =
        "https://github.com/skywars1133-art/vibecoding-ran-test/releases/download/puzzle-packs/",
    val puzzlePackConnectTimeoutMs: Int = 15_000,
    val puzzlePackReadTimeoutMs: Int = 30_000,
    /** Retries after a failed download, before waiting for the next daily run. */
    val puzzlePackMaxAttempts: Int = 3,
    val puzzlePackRetryBackoffMinutes: Long = 10,
    /** Stored sync-error text is truncated to this many characters. */
    val puzzlePackErrorMaxLength: Int = 300,
    /** SPEC 2: Wi-Fi only by default; the user can allow mobile data in Settings. */
    val wifiOnlyDownloadsByDefault: Boolean = true,
) {
    val puzzlePackRefreshIntervalMs: Long get() = puzzlePackRefreshHours * 60L * 60L * 1000L

    /** Centers of all generated packs: 400, 600, ... 2800 with the default config. */
    fun puzzlePackBandCenters(): List<Int> {
        if (puzzlePackBandWidth <= 0 || puzzlePackMaxRating < puzzlePackMinRating) return emptyList()
        return (puzzlePackMinRating..puzzlePackMaxRating step puzzlePackBandWidth).toList()
    }

    fun dailyCapSeconds(): Long? = dailyCapMinutes?.let { it * 60L }

    companion object {
        val DEFAULT = AppConfig()
    }
}
