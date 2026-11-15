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

/**
 * Remaining time at the granularity the widget shows. Whole minutes: `29 min`, `1 h 12 min` or
 * `2 h`. A partial minute still counts as one, so the card never reads `0 min` while time is left.
 */
sealed interface WidgetTime {
    data class Minutes(val minutes: Int) : WidgetTime
    data class Hours(val hours: Int, val minutes: Int) : WidgetTime
}

fun widgetTimeOf(remainingSeconds: Long): WidgetTime {
    val seconds = remainingSeconds.coerceAtLeast(0L)
    val totalMinutes = if (seconds == 0L) 0L else maxOf(1L, seconds / 60L)
    val hours = totalMinutes / 60L
    val minutes = totalMinutes % 60L
    return if (hours == 0L) {
        WidgetTime.Minutes(minutes.toInt())
    } else {
        WidgetTime.Hours(hours.toInt(), minutes.toInt())
    }
}

/**
 * First refresh tick strictly after [nowMillis], aligned to [intervalMillis] boundaries. Aligning to
 * the wall clock means a 60 s interval fires near :00 of every minute instead of 60 s after the
 * widget happened to be added. Pure so the scheduling edge cases are unit-tested.
 */
fun nextWidgetTickAt(nowMillis: Long, intervalMillis: Long): Long {
    val interval = intervalMillis.coerceAtLeast(1L)
    return nowMillis - nowMillis % interval + interval
}
