package com.chesslock.data

import com.chesslock.domain.entitlements.Entitlements
import com.chesslock.domain.entitlements.PremiumFeature
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EntitlementsRepositoryTest {

    @Test
    fun startsOnTheFreeTier() {
        val repository = EntitlementsRepository()
        assertEquals(Entitlements.FREE, repository.state.value)
        assertFalse(repository.state.value.isPremium)
    }

    @Test
    fun updatePublishesTheNewSnapshot() {
        val repository = EntitlementsRepository()
        repository.update(Entitlements(setOf(PremiumFeature.AD_FREE_HINTS)))

        assertTrue(repository.state.value.isPremium)
        assertTrue(repository.state.value.isUnlocked(PremiumFeature.AD_FREE_HINTS))

        repository.update(Entitlements.FREE)
        assertFalse(repository.state.value.isPremium)
    }
}
