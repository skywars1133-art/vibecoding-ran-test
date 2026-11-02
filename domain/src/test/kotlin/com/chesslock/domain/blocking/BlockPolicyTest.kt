package com.chesslock.domain.blocking

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BlockPolicyTest {

    private val rules = BlockRules(
        blockedCategories = setOf(BlockCategory.SOCIAL, BlockCategory.GAMES, BlockCategory.VIDEO),
    )

    private fun blocked(
        packageName: String,
        platformCategory: PlatformCategory = PlatformCategory.UNDEFINED,
        rules: BlockRules = this.rules,
        whitelisted: Set<String> = emptySet(),
    ): Boolean = BlockPolicy.isBlocked(
        packageName = packageName,
        platformCategory = platformCategory,
        rules = rules,
        ownPackageName = OWN,
        whitelistedPackages = whitelisted,
    )

    @Test
    fun `popular packages are classified without platform metadata`() {
        assertEquals(BlockCategory.SOCIAL, AppCategoryClassifier.classify("com.facebook.katana", PlatformCategory.UNDEFINED))
        assertEquals(BlockCategory.GAMES, AppCategoryClassifier.classify("com.supercell.clashofclans", PlatformCategory.UNDEFINED))
        assertEquals(BlockCategory.VIDEO, AppCategoryClassifier.classify("com.google.android.youtube", PlatformCategory.UNDEFINED))
        assertEquals(BlockCategory.COMMUNICATION, AppCategoryClassifier.classify("com.whatsapp", PlatformCategory.UNDEFINED))
        assertEquals(BlockCategory.SHOPPING, AppCategoryClassifier.classify("com.amazon.mShop.android.shopping", PlatformCategory.UNDEFINED))
    }

    @Test
    fun `platform category covers packages missing from the curated map`() {
        assertEquals(BlockCategory.GAMES, AppCategoryClassifier.classify("com.example.game", PlatformCategory.GAMES))
        assertEquals(BlockCategory.SOCIAL, AppCategoryClassifier.classify("com.example.social", PlatformCategory.SOCIAL))
        assertEquals(BlockCategory.OTHER, AppCategoryClassifier.classify("com.example.audio", PlatformCategory.AUDIO))
        assertEquals(BlockCategory.OTHER, AppCategoryClassifier.classify("com.example.unknown", PlatformCategory.UNDEFINED))
    }

    @Test
    fun `curated mapping wins over platform metadata`() {
        assertEquals(
            BlockCategory.COMMUNICATION,
            AppCategoryClassifier.classify("com.whatsapp", PlatformCategory.SOCIAL),
        )
    }

    @Test
    fun `unknown package with unknown category is only blocked when other is selected`() {
        assertFalse(blocked("com.example.unknown"))
        assertTrue(blocked("com.example.unknown", rules = rules.copy(blockedCategories = setOf(BlockCategory.OTHER))))
    }

    @Test
    fun `category not selected is allowed`() {
        val shoppingOnly = BlockRules(blockedCategories = setOf(BlockCategory.SHOPPING))
        assertFalse(blocked("com.facebook.katana", rules = shoppingOnly))
        assertTrue(blocked("com.amazon.mShop.android.shopping", rules = shoppingOnly))
    }

    @Test
    fun `always-blocked package ignores category selection`() {
        val custom = rules.copy(
            blockedCategories = emptySet(),
            alwaysBlockedPackages = setOf("com.example.distraction"),
        )
        assertTrue(blocked("com.example.distraction", rules = custom))
    }

    @Test
    fun `never-blocked package wins over always-blocked and category`() {
        val custom = rules.copy(
            alwaysBlockedPackages = setOf("com.facebook.katana"),
            neverBlockedPackages = setOf("com.facebook.katana"),
        )
        assertFalse(blocked("com.facebook.katana", rules = custom))
    }

    @Test
    fun `own app is never blocked`() {
        val custom = rules.copy(alwaysBlockedPackages = setOf(OWN))
        assertFalse(blocked(OWN, rules = custom))
    }

    @Test
    fun `critical system packages are never blocked`() {
        val custom = rules.copy(alwaysBlockedPackages = setOf(PHONE, LAUNCHER, SETTINGS))
        assertFalse(blocked(PHONE, rules = custom, whitelisted = setOf(PHONE)))
        assertFalse(blocked(LAUNCHER, rules = custom, whitelisted = setOf(LAUNCHER)))
        assertFalse(blocked(SETTINGS, rules = custom, whitelisted = setOf(SETTINGS)))
    }

    @Test
    fun `blank package name is allowed`() {
        assertFalse(blocked(""))
    }

    @Test
    fun `category storage keys round-trip`() {
        BlockCategory.entries.forEach { category ->
            assertEquals(category, BlockCategory.fromStorageKey(category.storageKey))
        }
        assertNull(BlockCategory.fromStorageKey(null))
        assertNull(BlockCategory.fromStorageKey("unknown"))
    }

    private companion object {
        const val OWN = "com.chesslock"
        const val PHONE = "com.android.dialer"
        const val LAUNCHER = "com.android.launcher3"
        const val SETTINGS = "com.android.settings"
    }
}
