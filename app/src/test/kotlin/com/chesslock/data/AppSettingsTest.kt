package com.chesslock.data

import com.chesslock.domain.blocking.BlockCategory
import com.chesslock.domain.config.AppConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppSettingsTest {

    private val config = AppConfig.DEFAULT

    @Test
    fun `defaults mirror the central config`() {
        val settings = AppSettings.defaults(config)
        assertEquals(config.blockingEnabledByDefault, settings.blockingEnabled)
        assertEquals(config.defaultDailyLimitMinutes, settings.dailyLimitMinutes)
        assertEquals(config.dailyCapMinutes, settings.dailyCapMinutes)
        assertEquals(config.defaultBlockedCategories, settings.blockedCategories)
        assertEquals(config.wifiOnlyDownloadsByDefault, settings.wifiOnlyDownloads)
        assertEquals(emptySet<String>(), settings.alwaysBlockedPackages)
        assertEquals(emptySet<String>(), settings.neverBlockedPackages)
    }

    @Test
    fun `daily limit and cap convert to seconds`() {
        val settings = AppSettings.defaults(config).copy(
            dailyLimitMinutes = 45,
            dailyCapMinutes = 90,
        )
        assertEquals(45 * 60L, settings.dailyLimitSeconds)
        assertEquals(90 * 60L, settings.dailyCapSeconds)
    }

    @Test
    fun `no cap means no cap seconds`() {
        val settings = AppSettings.defaults(config).copy(dailyCapMinutes = null)
        assertNull(settings.dailyCapSeconds)
    }

    @Test
    fun `per-app overrides report their state`() {
        val settings = AppSettings.defaults(config).copy(
            alwaysBlockedPackages = setOf("com.example.always"),
            neverBlockedPackages = setOf("com.example.never"),
        )
        assertEquals(AppBlockOverride.ALWAYS_BLOCK, settings.overrideFor("com.example.always"))
        assertEquals(AppBlockOverride.NEVER_BLOCK, settings.overrideFor("com.example.never"))
        assertEquals(AppBlockOverride.DEFAULT, settings.overrideFor("com.example.other"))
    }

    @Test
    fun `language defaults to system and round-trips storage keys`() {
        assertEquals(AppLanguage.SYSTEM, AppLanguage.DEFAULT)
        AppLanguage.entries.forEach { language ->
            assertEquals(language, AppLanguage.fromStorageKey(language.storageKey))
        }
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromStorageKey(null))
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromStorageKey("unknown"))
    }

    @Test
    fun `language tags drive the in-app locale override`() {
        assertNull(AppLanguage.SYSTEM.languageTag)
        assertEquals("en", AppLanguage.ENGLISH.languageTag)
        assertEquals("sk", AppLanguage.SLOVAK.languageTag)
    }

    @Test
    fun `default settings use cburnett and system language`() {
        val settings = AppSettings.defaults(config)
        assertEquals(PieceStyle.CBURNETT, settings.pieceStyle)
        assertEquals(AppLanguage.SYSTEM, settings.language)
    }

    @Test
    fun `default blocked categories are social games and video`() {
        assertEquals(
            setOf(BlockCategory.SOCIAL, BlockCategory.GAMES, BlockCategory.VIDEO),
            AppSettings.defaults(config).blockedCategories,
        )
    }
}
