package com.chesslock.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.chesslock.domain.config.AppConfig
import com.chesslock.domain.elo.EloCalculator
import com.chesslock.domain.elo.EloResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

private val Context.playerDataStore by preferencesDataStore(name = "player")

data class PlayerState(
    val elo: Int,
    val ratedPuzzlesPlayed: Int,
    val freeSkipsUsedToday: Int,
    val hintsUsedToday: Int,
    val placementCompleted: Boolean,
)

data class EloUpdate(
    val oldElo: Int,
    val newElo: Int,
) {
    val delta: Int get() = newElo - oldElo
}

/** Local-only chess progress. No app-usage data is stored or sent here. */
@Singleton
class PlayerStore @Inject constructor(
    @ApplicationContext private val context: Context,
    private val config: AppConfig,
) {
    private val eloKey = intPreferencesKey("elo")
    private val ratedCountKey = intPreferencesKey("rated_puzzles_played")
    private val skipDateKey = stringPreferencesKey("free_skip_date")
    private val freeSkipsUsedKey = intPreferencesKey("free_skips_used")
    private val hintDateKey = stringPreferencesKey("hint_date")
    private val hintsUsedKey = intPreferencesKey("hints_used")
    private val placementCompletedKey = booleanPreferencesKey("placement_completed")

    val state: Flow<PlayerState> = context.playerDataStore.data.map { preferences ->
        PlayerState(
            elo = preferences[eloKey] ?: config.eloStart,
            ratedPuzzlesPlayed = preferences[ratedCountKey] ?: 0,
            freeSkipsUsedToday = if (preferences[skipDateKey] == LocalDate.now().toString()) {
                preferences[freeSkipsUsedKey] ?: 0
            } else {
                0
            },
            hintsUsedToday = if (preferences[hintDateKey] == LocalDate.now().toString()) {
                preferences[hintsUsedKey] ?: 0
            } else {
                0
            },
            placementCompleted = preferences[placementCompletedKey] ?: false,
        )
    }

    suspend fun currentState(): PlayerState = state.first()

    suspend fun recordResult(puzzleRating: Int, result: EloResult): EloUpdate {
        var oldElo = config.eloStart
        var newElo = oldElo
        context.playerDataStore.edit { preferences ->
            oldElo = preferences[eloKey] ?: config.eloStart
            val played = preferences[ratedCountKey] ?: 0
            newElo = EloCalculator.newElo(oldElo, puzzleRating, played, result, config)
            preferences[eloKey] = newElo
            preferences[ratedCountKey] = played + 1
        }
        return EloUpdate(oldElo = oldElo, newElo = newElo)
    }

    /** Stores the ELO found by the placement test and stops asking for it on launch. */
    suspend fun completePlacement(elo: Int) {
        context.playerDataStore.edit { preferences ->
            preferences[eloKey] = elo.coerceIn(config.eloFloor, config.placementMaxRating)
            preferences[placementCompletedKey] = true
        }
    }

    suspend fun consumeFreeSkip() {
        context.playerDataStore.edit { preferences ->
            val today = LocalDate.now().toString()
            val used = if (preferences[skipDateKey] == today) preferences[freeSkipsUsedKey] ?: 0 else 0
            preferences[skipDateKey] = today
            preferences[freeSkipsUsedKey] = used + 1
        }
    }

    /** Records one hint obtained from a completed rewarded ad and returns today's new total. */
    suspend fun consumeHint(): Int {
        var used = 0
        context.playerDataStore.edit { preferences ->
            val today = LocalDate.now().toString()
            used = if (preferences[hintDateKey] == today) preferences[hintsUsedKey] ?: 0 else 0
            used += 1
            preferences[hintDateKey] = today
            preferences[hintsUsedKey] = used
        }
        return used
    }
}
