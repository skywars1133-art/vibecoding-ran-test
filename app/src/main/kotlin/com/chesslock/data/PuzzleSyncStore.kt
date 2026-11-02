package com.chesslock.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.chesslock.domain.config.AppConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.puzzleSyncDataStore by preferencesDataStore(name = "puzzle_sync")

/** Metadata about the last daily puzzle-pack download (SPEC 2). */
data class PuzzleSyncState(
    val lastSuccessEpochMs: Long = 0L,
    val lastBandCenter: Int? = null,
    val lastPackSize: Int = 0,
    val lastAddedCount: Int = 0,
    val lastError: String? = null,
) {
    val hasSynced: Boolean get() = lastSuccessEpochMs > 0L
}

@Singleton
class PuzzleSyncStore @Inject constructor(
    @ApplicationContext private val context: Context,
    private val config: AppConfig,
) {
    private val lastSuccessKey = longPreferencesKey("last_success_epoch_ms")
    private val lastBandKey = intPreferencesKey("last_band_center")
    private val lastPackSizeKey = intPreferencesKey("last_pack_size")
    private val lastAddedKey = intPreferencesKey("last_added_count")
    private val lastErrorKey = stringPreferencesKey("last_error")

    val state: Flow<PuzzleSyncState> = context.puzzleSyncDataStore.data.map { preferences ->
        PuzzleSyncState(
            lastSuccessEpochMs = preferences[lastSuccessKey] ?: 0L,
            lastBandCenter = preferences[lastBandKey],
            lastPackSize = preferences[lastPackSizeKey] ?: 0,
            lastAddedCount = preferences[lastAddedKey] ?: 0,
            lastError = preferences[lastErrorKey],
        )
    }

    suspend fun markSuccess(
        epochMs: Long,
        bandCenter: Int,
        packSize: Int,
        addedCount: Int,
    ) {
        context.puzzleSyncDataStore.edit { preferences ->
            preferences[lastSuccessKey] = epochMs
            preferences[lastBandKey] = bandCenter
            preferences[lastPackSizeKey] = packSize
            preferences[lastAddedKey] = addedCount
            preferences.remove(lastErrorKey)
        }
    }

    suspend fun markFailure(message: String) {
        context.puzzleSyncDataStore.edit { preferences ->
            preferences[lastErrorKey] = message.take(config.puzzlePackErrorMaxLength)
        }
    }
}
