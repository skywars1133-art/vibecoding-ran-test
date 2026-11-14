package com.chesslock.domain.budget

/**
 * How urgent the remaining daily budget is. Pure logic so the Home hero card, the ring and the
 * warning colours all agree; the UI maps each level to a colour.
 */
enum class BudgetUrgency {
    /** More time left than the highest warning threshold. */
    COMFORTABLE,

    /** At or below the highest warning threshold (5 minutes by default). */
    WARNING,

    /** At or below the lowest warning threshold (1 minute by default). */
    CRITICAL,

    /** No time left, so blocked apps show the block screen. */
    EMPTY,
    ;

    companion object {
        /**
         * Classifies [remainingSeconds] against the same thresholds that produce the warning
         * notifications, so the card turns amber exactly when the first warning fires. Negative or
         * zero time is always [EMPTY], and a missing threshold list stays [COMFORTABLE].
         */
        fun forRemaining(remainingSeconds: Long, warnAtSeconds: List<Long>): BudgetUrgency {
            if (remainingSeconds <= 0L) return EMPTY
            val thresholds = warnAtSeconds.filter { it > 0L }.sorted()
            if (thresholds.isEmpty()) return COMFORTABLE
            return when {
                remainingSeconds <= thresholds.first() -> CRITICAL
                remainingSeconds <= thresholds.last() -> WARNING
                else -> COMFORTABLE
            }
        }
    }
}
