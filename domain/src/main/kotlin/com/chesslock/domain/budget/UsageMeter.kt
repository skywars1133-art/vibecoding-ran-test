package com.chesslock.domain.budget

/**
 * Tracks blocked-app usage on top of the total that is already persisted.
 *
 * The blocking service counts in memory and flushes to storage every few seconds. Keeping that
 * arithmetic here (instead of spreading base/pending totals through the service) makes it
 * impossible for a late DataStore emission or a retried flush to charge the same seconds twice,
 * and lets the behaviour be unit-tested without Android.
 *
 * [usedSeconds] is the authoritative total for the current day: it never drops when a flush is
 * attempted, and it only moves when time is counted or the day is reset.
 *
 * Time arrives in milliseconds because that is what `SystemClock.elapsedRealtime()` returns. The
 * sub-second remainder is carried between ticks, so a poll every 2 s charges exactly 2 s instead of
 * rounding (or, worse, treating milliseconds as seconds).
 */
class UsageMeter {

    private var persistedSeconds = 0L
    private var unflushedSeconds = 0L
    private var remainderMillis = 0L

    /** Total seconds used today, including usage that has not been flushed yet. */
    val usedSeconds: Long get() = persistedSeconds + unflushedSeconds

    /** Seconds counted but not yet written to storage. */
    val unflushed: Long get() = unflushedSeconds

    /** Starts (or restarts) the day from the persisted total. */
    fun reset(persistedUsedSeconds: Long) {
        persistedSeconds = persistedUsedSeconds.coerceAtLeast(0L)
        unflushedSeconds = 0L
        remainderMillis = 0L
    }

    /**
     * Charges blocked time measured in milliseconds, never more than [maxCatchUpMillis] for one
     * tick and never more than [availableSeconds] left in the budget. Returns the whole seconds
     * actually counted (zero when nothing was charged).
     */
    fun count(elapsedMillis: Long, maxCatchUpMillis: Long, availableSeconds: Long): Long {
        if (elapsedMillis <= 0L) return 0L
        val cappedMillis = elapsedMillis.coerceAtMost(maxCatchUpMillis.coerceAtLeast(0L))
        val totalMillis = cappedMillis + remainderMillis
        val seconds = totalMillis / 1_000L
        remainderMillis = totalMillis % 1_000L
        if (seconds <= 0L || availableSeconds <= 0L) return 0L
        val counted = minOf(seconds, availableSeconds)
        unflushedSeconds += counted
        return counted
    }

    /** Drops the sub-second remainder when a blocked run ends, so it is not charged later. */
    fun endBlockedRun() {
        remainderMillis = 0L
    }

    /**
     * Hands the unflushed seconds to the caller for persisting. The total stays unchanged, so the
     * countdown does not jump if the write is slow. [restore] must be called if the write fails.
     */
    fun takeUnflushed(): Long {
        val seconds = unflushedSeconds
        unflushedSeconds = 0L
        persistedSeconds += seconds
        return seconds
    }

    /** Puts a failed flush back into the unflushed bucket without changing the total. */
    fun restore(seconds: Long) {
        if (seconds <= 0L) return
        persistedSeconds = (persistedSeconds - seconds).coerceAtLeast(0L)
        unflushedSeconds += seconds
    }
}
