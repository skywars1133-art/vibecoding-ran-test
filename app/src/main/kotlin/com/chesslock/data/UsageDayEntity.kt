package com.chesslock.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Per-day totals for the local statistics screen (SPEC 5). App-usage history never leaves the
 * device. Written best-effort by [BudgetStore]; the budget itself keeps working if this fails.
 */
@Entity(tableName = "usage_days")
data class UsageDayEntity(
    /** Local date in ISO form (YYYY-MM-DD). */
    @PrimaryKey val date: String,
    val usedSeconds: Long,
    val earnedSeconds: Long,
)
