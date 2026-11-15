package com.chesslock.widget

import com.chesslock.domain.budget.BudgetState
import com.chesslock.domain.budget.BudgetUrgency
import com.chesslock.domain.budget.TimeBudget
import kotlin.math.roundToInt

/**
 * What the home-screen widget shows. Pure so the arithmetic and urgency mapping are unit-tested
 * without Android. [progressPercent] is clamped because the budget can be over-used on a day whose
 * limit was lowered after some time was already spent.
 */
data class WidgetSnapshot(
    val remainingSeconds: Long,
    val totalSeconds: Long,
    val elo: Int,
    val urgency: BudgetUrgency,
) {
    val fraction: Float
        get() = if (totalSeconds <= 0L) {
            0f
        } else {
            (remainingSeconds.toFloat() / totalSeconds).coerceIn(0f, 1f)
        }

    val progressPercent: Int get() = (fraction * 100f).roundToInt()
}

fun buildWidgetSnapshot(
    budget: BudgetState,
    limitSeconds: Long,
    capSeconds: Long?,
    elo: Int,
    warnAtSeconds: List<Long>,
): WidgetSnapshot {
    val remaining = TimeBudget.remainingSeconds(budget, limitSeconds, capSeconds)
    return WidgetSnapshot(
        remainingSeconds = remaining,
        totalSeconds = TimeBudget.totalSeconds(budget, limitSeconds, capSeconds),
        elo = elo,
        urgency = BudgetUrgency.forRemaining(remaining, warnAtSeconds),
    )
}
