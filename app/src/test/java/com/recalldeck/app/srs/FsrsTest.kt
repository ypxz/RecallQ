package com.recalldeck.app.srs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Parity vectors for the FSRS-6 port. Expected values are derived from the formulas of the
 * MIT-licensed reference at https://github.com/open-spaced-repetition/FSRS-Kotlin using the
 * FSRS-6 default parameters (independently computed; see docs/DECISIONS.md).
 */
class FsrsTest {

    private val fsrs = Fsrs()

    @Test
    fun `initial state parity vectors`() {
        assertEquals(Fsrs.MemoryState(0.21, 6.41), fsrs.initialState(Grade.AGAIN))
        assertEquals(Fsrs.MemoryState(1.29, 5.11), fsrs.initialState(Grade.HARD))
        assertEquals(Fsrs.MemoryState(2.31, 2.12), fsrs.initialState(Grade.GOOD))
        assertEquals(Fsrs.MemoryState(8.3, 1.0), fsrs.initialState(Grade.EASY))
    }

    @Test
    fun `next difficulty parity vectors`() {
        val s = Fsrs.MemoryState(stability = 2.0, difficulty = 5.0)
        assertEquals(8.35, fsrs.shortTermState(s, Grade.AGAIN).difficulty, 1e-9)
        assertEquals(6.67, fsrs.shortTermState(s, Grade.HARD).difficulty, 1e-9)
        assertEquals(5.0, fsrs.shortTermState(s, Grade.GOOD).difficulty, 1e-9)
        assertEquals(3.32, fsrs.shortTermState(s, Grade.EASY).difficulty, 1e-9)
    }

    @Test
    fun `short term stability parity vectors`() {
        val s = Fsrs.MemoryState(stability = 2.0, difficulty = 5.0)
        assertEquals(0.68, fsrs.shortTermState(s, Grade.AGAIN).stability, 1e-9)
        assertEquals(1.17, fsrs.shortTermState(s, Grade.HARD).stability, 1e-9)
        assertEquals(2.01, fsrs.shortTermState(s, Grade.GOOD).stability, 1e-9)
        assertEquals(3.45, fsrs.shortTermState(s, Grade.EASY).stability, 1e-9)
    }

    @Test
    fun `recall stability parity vectors`() {
        val state = Fsrs.MemoryState(stability = 10.0, difficulty = 5.0)
        assertEquals(Fsrs.MemoryState(23.25, 6.67), fsrs.nextState(state, 10.0, Grade.HARD))
        assertEquals(Fsrs.MemoryState(32.03, 5.0), fsrs.nextState(state, 10.0, Grade.GOOD))
        assertEquals(Fsrs.MemoryState(51.25, 3.32), fsrs.nextState(state, 10.0, Grade.EASY))
    }

    @Test
    fun `forget stability parity vector`() {
        val state = Fsrs.MemoryState(stability = 10.0, difficulty = 5.0)
        assertEquals(Fsrs.MemoryState(1.39, 8.35), fsrs.nextState(state, 10.0, Grade.AGAIN))
    }

    @Test
    fun `forgetting curve is 90 percent when elapsed equals stability`() {
        assertEquals(0.9, fsrs.forgettingCurve(5.0, 5.0), 1e-9)
        assertEquals(0.9, fsrs.forgettingCurve(123.0, 123.0), 1e-9)
        assertEquals(1.0, fsrs.forgettingCurve(0.0, 5.0), 1e-9)
    }

    @Test
    fun `forgetting curve decreases with time`() {
        val early = fsrs.forgettingCurve(1.0, 10.0)
        val late = fsrs.forgettingCurve(100.0, 10.0)
        assertTrue(early > late)
    }

    @Test
    fun `next interval equals stability at default retention`() {
        assertEquals(1, fsrs.nextIntervalDays(1.0))
        assertEquals(10, fsrs.nextIntervalDays(10.0))
        assertEquals(100, fsrs.nextIntervalDays(100.0))
        assertEquals(1, fsrs.nextIntervalDays(0.5))
    }

    @Test
    fun `next interval responds to retention target`() {
        assertEquals(33, Fsrs(requestRetention = 0.8).nextIntervalDays(10.0))
        assertEquals(4, Fsrs(requestRetention = 0.95).nextIntervalDays(10.0))
    }

    @Test
    fun `next interval is clamped to max`() {
        assertEquals(36500, fsrs.nextIntervalDays(1e9))
        assertEquals(30, fsrs.nextIntervalDays(1e9, maxInterval = 30))
    }

    @Test
    fun `repeated good reviews match reference sequence`() {
        var state = fsrs.initialState(Grade.GOOD)
        assertEquals(Fsrs.MemoryState(2.31, 2.12), state)

        val expected = listOf(
            Fsrs.MemoryState(10.97, 2.12),
            Fsrs.MemoryState(46.25, 2.12),
            Fsrs.MemoryState(162.62, 2.12),
        )
        for (exp in expected) {
            val interval = fsrs.nextIntervalDays(state.stability)
            state = fsrs.nextState(state, interval.toDouble(), Grade.GOOD)
            assertEquals(exp, state)
        }
    }

    @Test
    fun `fuzz keeps interval within 5 percent band`() {
        val base = fsrs.nextIntervalDays(100.0)
        for (fuzz in listOf(0.0, 0.25, 0.5, 0.75, 0.999)) {
            val fuzzed = fsrs.nextIntervalDays(100.0, fuzzFactor = fuzz)
            assertTrue("fuzzed=$fuzzed base=$base", fuzzed in (base * 0.95 - 1).toInt()..(base * 1.05 + 1).toInt())
        }
    }

    @Test
    fun `stability never drops below minimum`() {
        val state = Fsrs.MemoryState(stability = 0.01, difficulty = 10.0)
        assertTrue(fsrs.nextState(state, 1000.0, Grade.AGAIN).stability >= Fsrs.MIN_STABILITY)
        assertTrue(fsrs.shortTermState(state, Grade.AGAIN).stability >= Fsrs.MIN_STABILITY)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rejects wrong parameter count`() {
        Fsrs(params = listOf(1.0, 2.0))
    }
}
