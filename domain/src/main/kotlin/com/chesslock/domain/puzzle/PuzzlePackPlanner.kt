package com.chesslock.domain.puzzle

import com.chesslock.domain.config.AppConfig
import kotlin.math.roundToInt

/**
 * Maps a player's ELO onto one of the pre-generated Lichess puzzle packs (SPEC 2, Phase 2).
 *
 * The pipeline publishes one file per 200-point band center (400, 600, ... 2800). Each pack holds
 * puzzles from center - 300 to center + 300, so it always has easier and harder material around
 * the player's current level.
 */
object PuzzlePackPlanner {

    /** Nearest band center for [rating], clamped to the generated range. */
    fun bandCenter(rating: Int, config: AppConfig = AppConfig.DEFAULT): Int {
        val centers = config.puzzlePackBandCenters()
        require(centers.isNotEmpty()) { "No puzzle pack band centers configured" }
        val first = centers.first()
        val last = centers.last()
        val clamped = rating.coerceIn(first, last)
        val steps = ((clamped - first).toDouble() / config.puzzlePackBandWidth).roundToInt()
        return (first + steps * config.puzzlePackBandWidth).coerceIn(first, last)
    }

    /** Ratings covered by the pack around [center]: [center - spread, center + spread). */
    fun bandRange(center: Int, config: AppConfig = AppConfig.DEFAULT): IntRange =
        (center - config.puzzlePackSpread) until (center + config.puzzlePackSpread)

    fun contains(rating: Int, center: Int, config: AppConfig = AppConfig.DEFAULT): Boolean =
        rating in bandRange(center, config)

    fun fileName(center: Int): String = "puzzles-$center.csv"

    /**
     * Download URL for one band. [version] is a cache-busting value (the app passes the current
     * UTC day); GitHub release assets keep the same URL when they are replaced by the daily job.
     */
    fun packUrl(baseUrl: String, center: Int, version: Long? = null): String {
        val file = baseUrl.trimEnd('/') + "/" + fileName(center)
        return if (version == null) file else "$file?v=$version"
    }

    /**
     * A sync is due when it never succeeded or the last success is at least [intervalMs] old.
     * A clock moved backwards (negative age) does not force repeated downloads.
     */
    fun isDue(lastSuccessEpochMs: Long, nowEpochMs: Long, intervalMs: Long): Boolean =
        lastSuccessEpochMs <= 0L || nowEpochMs - lastSuccessEpochMs >= intervalMs
}
