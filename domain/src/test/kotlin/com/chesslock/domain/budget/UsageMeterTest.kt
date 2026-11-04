package com.chesslock.domain.budget

import org.junit.Assert.assertEquals
import org.junit.Test

class UsageMeterTest {

    @Test
    fun `starts from the persisted total`() {
        val meter = UsageMeter()
        meter.reset(600)

        assertEquals(600L, meter.usedSeconds)
        assertEquals(0L, meter.unflushed)
    }

    @Test
    fun `counts elapsed time on top of the persisted total`() {
        val meter = UsageMeter()
        meter.reset(600)

        val counted = meter.count(elapsedSeconds = 2, availableSeconds = 1_200)

        assertEquals(2L, counted)
        assertEquals(602L, meter.usedSeconds)
        assertEquals(2L, meter.unflushed)
    }

    @Test
    fun `never counts more than the remaining budget`() {
        val meter = UsageMeter()
        meter.reset(1_595)

        val counted = meter.count(elapsedSeconds = 10, availableSeconds = 5)

        assertEquals(5L, counted)
        assertEquals(1_600L, meter.usedSeconds)
    }

    @Test
    fun `ignores non-positive input`() {
        val meter = UsageMeter()
        meter.reset(100)

        assertEquals(0L, meter.count(0, 100))
        assertEquals(0L, meter.count(-5, 100))
        assertEquals(0L, meter.count(5, 0))
        assertEquals(100L, meter.usedSeconds)
    }

    @Test
    fun `a flush does not change the displayed total`() {
        val meter = UsageMeter()
        meter.reset(100)
        meter.count(2, 1_000)
        meter.count(2, 1_000)
        val before = meter.usedSeconds

        val flushed = meter.takeUnflushed()

        assertEquals(4L, flushed)
        assertEquals(before, meter.usedSeconds)
        assertEquals(0L, meter.unflushed)
    }

    @Test
    fun `usage counted after a flush is not double counted`() {
        val meter = UsageMeter()
        meter.reset(100)
        meter.count(2, 1_000)
        meter.takeUnflushed()

        meter.count(2, 1_000)

        assertEquals(104L, meter.usedSeconds)
        assertEquals(2L, meter.unflushed)
    }

    @Test
    fun `a failed flush restores the seconds exactly once`() {
        val meter = UsageMeter()
        meter.reset(100)
        meter.count(2, 1_000)
        meter.count(3, 1_000)
        val before = meter.usedSeconds

        val flushed = meter.takeUnflushed()
        meter.restore(flushed)

        assertEquals(before, meter.usedSeconds)
        assertEquals(5L, meter.unflushed)
    }

    @Test
    fun `reset clears unflushed usage from the previous day`() {
        val meter = UsageMeter()
        meter.reset(1_000)
        meter.count(30, 1_000)

        meter.reset(0)

        assertEquals(0L, meter.usedSeconds)
        assertEquals(0L, meter.unflushed)
    }
}
