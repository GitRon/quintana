package io.github.gitron.quintana

import org.junit.Assert.assertEquals
import org.junit.Test

class TimerSetTest {
    @Test
    fun `standard set has the values of the specification`() {
        val standard = TimerSet.standard()
        assertEquals("Standard", standard.name)
        assertEquals(180, standard.workSeconds)
        assertEquals(60, standard.restSeconds)
        assertEquals(10, standard.rounds)
    }

    @Test
    fun `total without lead-in has no rest after the last round`() {
        val set = TimerSet(name = "Tabata", workSeconds = 20, restSeconds = 10, rounds = 8)
        assertEquals(8 * 20 + 7 * 10, set.totalSeconds(0))
        assertEquals(8 * 20 + 7 * 10 + 10, set.totalSeconds(10))
    }

    @Test
    fun `steppers move on their grid and stop at the limits`() {
        assertEquals(125, Limits.work.move(120, 1))
        assertEquals(5, Limits.work.move(5, -1))
        assertEquals(3600, Limits.work.move(3595, 6))
        assertEquals(0, Limits.rest.move(5, -1))
        assertEquals(99, Limits.rounds.move(99, 1))
        assertEquals(1, Limits.rounds.move(1, -1))
        assertEquals(0, Limits.preRoundWarning.move(1, -1))
        assertEquals(30, Limits.preRoundWarning.move(30, 1))
    }

    @Test
    fun `values off the grid move to the next grid point first`() {
        assertEquals(10, Limits.work.move(7, 1))
        assertEquals(5, Limits.work.move(7, -1))
        assertEquals(15, Limits.work.move(7, 2))
        assertEquals(7, Limits.work.move(7, 0))
    }

    @Test
    fun `clamped keeps values in range`() {
        val wild = TimerSet(name = "x", workSeconds = 0, restSeconds = 9999, rounds = 500).clamped()
        assertEquals(5, wild.workSeconds)
        assertEquals(3600, wild.restSeconds)
        assertEquals(99, wild.rounds)
    }

    @Test
    fun `empty name becomes the first free Set N`() {
        assertEquals("Sparring", nameOrDefault("  Sparring ", emptyList()))
        assertEquals("Set 1", nameOrDefault("", listOf("Standard")))
        assertEquals("Set 2", nameOrDefault(" ", listOf("Set 1", "Set 3")))
    }

    @Test
    fun `durations are formatted for the screens`() {
        assertEquals("2:00", formatShort(120))
        assertEquals("0:05", formatShort(5))
        assertEquals("35:10", formatShort(2110))
        assertEquals("1:02:30", formatShort(3750))
        assertEquals("01:42", formatClock(102))
        assertEquals("00:00", formatClock(0))
        assertEquals("1:00:00", formatClock(3600))
    }

    @Test
    fun `countdowns round the remaining seconds up`() {
        assertEquals(0, secondsLeft(0))
        assertEquals(1, secondsLeft(1))
        assertEquals(1, secondsLeft(1000))
        assertEquals(2, secondsLeft(1001))
        assertEquals(0, secondsLeft(-50))
    }
}
