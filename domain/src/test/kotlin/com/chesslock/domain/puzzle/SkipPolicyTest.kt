package com.chesslock.domain.puzzle

import com.chesslock.domain.config.AppConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SkipPolicyTest {
    private val config = AppConfig.DEFAULT

    @Test
    fun oneFreeSkipPerDay() {
        assertEquals(SkipKind.FREE, SkipPolicy.availableSkip(0, 10, config))
        assertNull(SkipPolicy.availableSkip(1, 10, config))
    }

    @Test
    fun skipUnlocksAfterTwentyMinutesEvenWithoutFreeSkips() {
        val justBefore = 20 * 60L - 1
        val exactly = 20 * 60L
        assertNull(SkipPolicy.availableSkip(1, justBefore, config))
        assertEquals(SkipKind.TIMEOUT, SkipPolicy.availableSkip(1, exactly, config))
    }

    @Test
    fun timeoutSkipIsPreferredSoFreeSkipIsKept() {
        assertEquals(SkipKind.TIMEOUT, SkipPolicy.availableSkip(0, 25 * 60L, config))
    }

    @Test
    fun limitsComeFromConfig() {
        val custom = config.copy(freeSkipsPerDay = 3, skipUnlockAfterMinutes = 5)
        assertEquals(SkipKind.FREE, SkipPolicy.availableSkip(2, 0, custom))
        assertNull(SkipPolicy.availableSkip(3, 299, custom))
        assertEquals(SkipKind.TIMEOUT, SkipPolicy.availableSkip(3, 300, custom))
    }
}
