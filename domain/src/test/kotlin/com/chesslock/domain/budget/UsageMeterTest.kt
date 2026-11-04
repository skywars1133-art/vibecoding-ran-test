package com.chesslock.domain.budget

import org.junit.Assert.assertEquals
import org.junit.Test

class UsageMeterTest {

    private val maxCatchUpMillis = 10_000L

    private fun UsageMeter.countTick(elapsedMillis: Long, availableSeconds: Long = 10_000) =
        count(elapsedMillis, maxCatchUpMillis, availableSeconds)

    @Test
    fun `starts from the persisted total`() {
        val meter = UsageMeter()
        meter.reset(600)

        assertEquals(600L, meter.usedSeconds)
        assertEquals(0L, meter.unflushed)
    }

    @Test
    fun `a two second poll charges two seconds not two thousand`() {
        val meter = UsageMeter()
        meter.reset(600)

        val counted = meter.countTick(elapsedMillis = 2_000)

        assertEquals(2L, counted)
        assertEquals(602L, meter.usedSeconds)
        assertEquals(2L, meter.unflushed)
    }

    @Test
    fun `five two second polls charge ten seconds`() {
        val meter = UsageMeter()
        meter.reset(0)

        repeat(5) { meter.countTick(elapsedMillis = 2_000) }

        assertEquals(10L, meter.usedSeconds)
    }

    @Test
    fun `sub-second remainders are carried between ticks`() {
        val meter = UsageMeter()
        meter.reset(0)

        assertEquals(1L, meter.countTick(elapsedMillis = 1_500))
        assertEquals(2L, meter.countTick(elapsedMillis = 1_500))
        assertEquals(3L, meter.usedSeconds)
    }

    @Test
    fun `a long stall is capped but the remainder is kept`() {
        val meter = UsageMeter()
        meter.reset(0)

        assertEquals(10L, meter.countTick(elapsedMillis = 30_000))
        assertEquals(10L, meter.usedSeconds)
    }

    @Test
    fun `never counts more than the remaining budget`() {
        val meter = UsageMeter()
        meter.reset(1_595)

        val counted = meter.countTick(elapsedMillis = 10_000, availableSeconds = 5)

        assertEquals(5L, counted)
        assertEquals(1_600L, meter.usedSeconds)
    }

    @Test
    fun `ignores non-positive input`() {
        val meter = UsageMeter()
        meter.reset(100)

        assertEquals(0L, meter.countTick(elapsedMillis = 0))
        assertEquals(0L, meter.countTick(elapsedMillis = -5))
        assertEquals(0L, meter.countTick(elapsedMillis = 5_000, availableSeconds = 0))
        assertEquals(100L, meter.usedSeconds)
    }

    @Test
    fun `ending a blocked run drops the sub-second remainder`() {
        val meter = UsageMeter()
        meter.reset(0)
        meter.countTick(elapsedMillis = 1_500)
        meter.endBlockedRun()

        assertEquals(1L, meter.countTick(elapsedMillis = 1_000))
        assertEquals(2L, meter.usedSeconds)
    }

    @Test
    fun `a flush does not change the displayed total`() {
        val meter = UsageMeter()
        meter.reset(100)
        meter.countTick(elapsedMillis = 2_000)
        meter.countTick(elapsedMillis = 2_000)
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
        meter.countTick(elapsedMillis = 2_000)
        meter.takeUnflushed()

        meter.countTick(elapsedMillis = 2_000)

        assertEquals(104L, meter.usedSeconds)
        assertEquals(2L, meter.unflushed)
    }

    @Test
    fun `a failed flush restores the seconds exactly once`() {
        val meter = UsageMeter()
        meter.reset(100)
        meter.countTick(elapsedMillis = 2_000)
        meter.countTick(elapsedMillis = 3_000)
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
        meter.countTick(elapsedMillis = 30_000)

        meter.reset(0)

        assertEquals(0L, meter.usedSeconds)
        assertEquals(0L, meter.unflushed)
    }
}
