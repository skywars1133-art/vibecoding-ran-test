package com.chesslock.ui.common

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.Locale

class TimeFormatTest {

    private lateinit var previous: Locale

    @Before
    fun setUp() {
        previous = Locale.getDefault()
        Locale.setDefault(Locale.US)
    }

    @After
    fun tearDown() {
        Locale.setDefault(previous)
    }

    @Test
    fun `zero renders as zero minutes`() {
        assertEquals("0:00", formatClock(0))
    }

    @Test
    fun `seconds and minutes use a two-digit seconds field`() {
        assertEquals("0:05", formatClock(5))
        assertEquals("0:59", formatClock(59))
        assertEquals("1:00", formatClock(60))
        assertEquals("59:59", formatClock(3599))
    }

    @Test
    fun `an hour or more adds the hours field`() {
        assertEquals("1:00:00", formatClock(3600))
        assertEquals("1:01:01", formatClock(3661))
        assertEquals("2:30:00", formatClock(9000))
    }

    @Test
    fun `negative values are clamped`() {
        assertEquals("0:00", formatClock(-1))
        assertEquals("0:00", formatClock(Long.MIN_VALUE))
    }
}
