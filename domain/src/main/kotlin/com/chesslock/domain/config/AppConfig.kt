package com.chesslock.domain.config

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
) {
    fun dailyCapSeconds(): Long? = dailyCapMinutes?.let { it * 60L }

    companion object {
        val DEFAULT = AppConfig()
    }
}
