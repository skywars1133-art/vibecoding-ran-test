package com.chesslock.domain.entitlements

import com.chesslock.domain.config.AppConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EntitlementsTest {
    private val config = AppConfig.DEFAULT

    @Test
    fun freeTierUnlocksNothing() {
        val free = Entitlements.FREE
        assertFalse(free.isPremium)
        PremiumFeature.entries.forEach { assertFalse(free.isUnlocked(it)) }
    }

    @Test
    fun unlockedFeaturesAreReported() {
        val premium = Entitlements(setOf(PremiumFeature.AD_FREE_HINTS))
        assertTrue(premium.isPremium)
        assertTrue(premium.isUnlocked(PremiumFeature.AD_FREE_HINTS))
        assertFalse(premium.isUnlocked(PremiumFeature.EXTRA_SKIPS))
    }

    @Test
    fun hintsRequireAdOnlyOnFreeTier() {
        assertTrue(EntitlementRules.hintsRequireAd(Entitlements.FREE))
        assertFalse(
            EntitlementRules.hintsRequireAd(
                Entitlements(setOf(PremiumFeature.AD_FREE_HINTS)),
            ),
        )
    }

    @Test
    fun extraSkipsAddTheConfiguredBonus() {
        assertEquals(
            config.freeSkipsPerDay,
            EntitlementRules.freeSkipsPerDay(config, Entitlements.FREE),
        )
        assertEquals(
            config.freeSkipsPerDay + config.premiumExtraSkipsPerDay,
            EntitlementRules.freeSkipsPerDay(
                config,
                Entitlements(setOf(PremiumFeature.EXTRA_SKIPS)),
            ),
        )

        val custom = config.copy(freeSkipsPerDay = 2, premiumExtraSkipsPerDay = 5)
        assertEquals(
            7,
            EntitlementRules.freeSkipsPerDay(
                custom,
                Entitlements(setOf(PremiumFeature.EXTRA_SKIPS)),
            ),
        )
    }
}
