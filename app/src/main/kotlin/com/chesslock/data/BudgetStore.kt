package com.chesslock.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.chesslock.domain.budget.BudgetState
import com.chesslock.domain.budget.TimeBudget
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

private val Context.budgetDataStore by preferencesDataStore(name = "budget")

/**
 * Persists the one shared daily budget (SPEC 1). Usage is recorded by the blocking service,
 * earned time by the puzzle screen. All date handling goes through [TimeBudget].
 */
@Singleton
class BudgetStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val dateKey = stringPreferencesKey("date")
    private val earnedKey = longPreferencesKey("earned_seconds")
    private val usedKey = longPreferencesKey("used_seconds")

    val state: Flow<BudgetState> = context.budgetDataStore.data.map { preferences ->
        preferences.toBudgetState(LocalDate.now())
    }

    suspend fun currentState(today: LocalDate = LocalDate.now()): BudgetState = state.first()

    suspend fun addUsage(seconds: Long, today: LocalDate = LocalDate.now()) {
        if (seconds <= 0L) return
        context.budgetDataStore.edit { preferences ->
            val state = preferences.toBudgetState(today)
            preferences.write(state)
            preferences[usedKey] = state.usedSeconds + seconds
        }
    }

    suspend fun addEarned(
        seconds: Long,
        limitSeconds: Long,
        capSeconds: Long?,
        today: LocalDate = LocalDate.now(),
    ) {
        if (seconds <= 0L) return
        context.budgetDataStore.edit { preferences ->
            val state = preferences.toBudgetState(today)
            val earned = TimeBudget.addEarned(state, seconds, limitSeconds, capSeconds)
            preferences.write(earned)
        }
    }

    private fun androidx.datastore.preferences.core.Preferences.toBudgetState(today: LocalDate): BudgetState {
        val storedDate = this[dateKey]?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: today
        val state = BudgetState(
            date = storedDate,
            earnedSeconds = this[earnedKey] ?: 0L,
            usedSeconds = this[usedKey] ?: 0L,
        )
        return TimeBudget.rollover(state, today)
    }

    private fun androidx.datastore.preferences.core.MutablePreferences.write(state: BudgetState) {
        this[dateKey] = state.date.toString()
        this[earnedKey] = state.earnedSeconds
        this[usedKey] = state.usedSeconds
    }
}
