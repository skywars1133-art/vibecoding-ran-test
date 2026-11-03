package com.chesslock.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.chesslock.domain.blocking.BlockCategory
import com.chesslock.domain.config.AppConfig
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
    private val config: AppConfig,
) {
    private val pieceStyleKey = stringPreferencesKey("piece_style")
    private val languageKey = stringPreferencesKey("app_language")
    private val themeModeKey = stringPreferencesKey("theme_mode")
    private val wifiOnlyDownloadsKey = booleanPreferencesKey("wifi_only_downloads")
    private val blockingEnabledKey = booleanPreferencesKey("blocking_enabled")
    private val dailyLimitKey = intPreferencesKey("daily_limit_minutes")
    private val dailyCapKey = intPreferencesKey("daily_cap_minutes")
    private val blockedCategoriesKey = stringSetPreferencesKey("blocked_categories")
    private val alwaysBlockedKey = stringSetPreferencesKey("always_blocked_packages")
    private val neverBlockedKey = stringSetPreferencesKey("never_blocked_packages")
    private val permissionsOnboardingSeenKey = booleanPreferencesKey("permissions_onboarding_seen")

    val settings: Flow<AppSettings> = context.settingsDataStore.data.map { preferences ->
        val storedCategories = preferences[blockedCategoriesKey]
        val categories = if (storedCategories == null) {
            config.defaultBlockedCategories
        } else {
            storedCategories.mapNotNull(BlockCategory::fromStorageKey).toSet()
        }
        val storedCap = if (preferences.contains(dailyCapKey)) {
            preferences[dailyCapKey]?.takeIf { it > 0 }
        } else {
            config.dailyCapMinutes
        }
        AppSettings(
            pieceStyle = PieceStyle.fromStorageKey(preferences[pieceStyleKey]),
            language = AppLanguage.fromStorageKey(preferences[languageKey]),
            themeMode = ThemeMode.fromStorageKey(preferences[themeModeKey]),
            wifiOnlyDownloads = preferences[wifiOnlyDownloadsKey] ?: config.wifiOnlyDownloadsByDefault,
            blockingEnabled = preferences[blockingEnabledKey] ?: config.blockingEnabledByDefault,
            dailyLimitMinutes = preferences[dailyLimitKey] ?: config.defaultDailyLimitMinutes,
            dailyCapMinutes = storedCap,
            blockedCategories = categories,
            alwaysBlockedPackages = preferences[alwaysBlockedKey] ?: emptySet(),
            neverBlockedPackages = preferences[neverBlockedKey] ?: emptySet(),
            permissionsOnboardingSeen = preferences[permissionsOnboardingSeenKey] ?: false,
        )
    }

    suspend fun setPieceStyle(style: PieceStyle) {
        context.settingsDataStore.edit { preferences ->
            preferences[pieceStyleKey] = style.storageKey
        }
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.settingsDataStore.edit { preferences ->
            preferences[languageKey] = language.storageKey
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.settingsDataStore.edit { preferences ->
            preferences[themeModeKey] = mode.storageKey
        }
    }

    suspend fun setWifiOnlyDownloads(enabled: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[wifiOnlyDownloadsKey] = enabled
        }
    }

    suspend fun setBlockingEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[blockingEnabledKey] = enabled
        }
    }

    suspend fun setDailyLimitMinutes(minutes: Int) {
        context.settingsDataStore.edit { preferences ->
            preferences[dailyLimitKey] = minutes.coerceAtLeast(1)
        }
    }

    suspend fun setDailyCapMinutes(minutes: Int?) {
        context.settingsDataStore.edit { preferences ->
            if (minutes == null) {
                preferences[dailyCapKey] = NO_CAP
            } else {
                preferences[dailyCapKey] = minutes.coerceAtLeast(1)
            }
        }
    }

    suspend fun setBlockedCategory(category: BlockCategory, blocked: Boolean) {
        context.settingsDataStore.edit { preferences ->
            val current = preferences[blockedCategoriesKey]
                ?: config.defaultBlockedCategories.map { it.storageKey }.toSet()
            preferences[blockedCategoriesKey] = if (blocked) {
                current + category.storageKey
            } else {
                current - category.storageKey
            }
        }
    }

    suspend fun setPermissionsOnboardingSeen(seen: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[permissionsOnboardingSeenKey] = seen
        }
    }

    suspend fun setAppOverride(packageName: String, override: AppBlockOverride) {
        context.settingsDataStore.edit { preferences ->
            var always = preferences[alwaysBlockedKey] ?: emptySet()
            var never = preferences[neverBlockedKey] ?: emptySet()
            always = always - packageName
            never = never - packageName
            when (override) {
                AppBlockOverride.DEFAULT -> Unit
                AppBlockOverride.ALWAYS_BLOCK -> always = always + packageName
                AppBlockOverride.NEVER_BLOCK -> never = never + packageName
            }
            preferences[alwaysBlockedKey] = always
            preferences[neverBlockedKey] = never
        }
    }

    private companion object {
        /** Stored cap value meaning "no cap" (a Preferences int cannot hold null). */
        const val NO_CAP = -1
    }
}
