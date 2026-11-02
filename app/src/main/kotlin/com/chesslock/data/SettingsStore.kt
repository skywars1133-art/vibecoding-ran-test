package com.chesslock.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

/** User preferences. Local only; none of this leaves the device. */
@Singleton
class SettingsStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val pieceStyleKey = stringPreferencesKey("piece_style")

    val settings: Flow<AppSettings> = context.settingsDataStore.data.map { preferences ->
        AppSettings(
            pieceStyle = PieceStyle.fromStorageKey(preferences[pieceStyleKey]),
        )
    }

    suspend fun setPieceStyle(style: PieceStyle) {
        context.settingsDataStore.edit { preferences ->
            preferences[pieceStyleKey] = style.storageKey
        }
    }
}
