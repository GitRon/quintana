package io.github.gitron.quintana.timer

import io.github.gitron.quintana.TimerSet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RunTest {
    @Test
    fun `clock counts from the start`() {
        val clock = RunClock.startedAt(1_000)
        assertEquals(0L, clock.elapsed(1_000))
        assertEquals(1_800_000L, clock.elapsed(1_801_000))
        assertFalse(clock.isPaused)
    }

    @Test
    fun `paused time does not count`() {
        val paused = RunClock.startedAt(0).pause(10_000)
        assertTrue(paused.isPaused)
        assertEquals(10_000L, paused.elapsed(500_000))

        val resumed = paused.resume(60_000)
        assertEquals(15_000L, resumed.elapsed(65_000))
    }

    @Test
    fun `pausing twice or resuming a running clock changes nothing`() {
        val paused = RunClock.startedAt(0).pause(10_000)
        assertEquals(paused, paused.pause(20_000))
        val running = RunClock.startedAt(0)
        assertEquals(running, running.resume(5_000))
    }

    private val set = TimerSet(name = "Test", workSeconds = 20, restSeconds = 15, rounds = 3)

    @Test
    fun `run is finished at the end of its schedule`() {
        val run = Run.start(set, preRoundWarningSeconds = 10, nowMillis = 0)
        assertFalse(run.isFinished(99_999))
        assertTrue(run.isFinished(100_000))
        assertEquals(Phase.WORK, run.snapshot(10_000).phase)
        assertEquals(Phase.DONE, run.copy(finished = true).snapshot(10_000).phase)
    }

    @Test
    fun `pause freezes the run and resume continues it`() {
        val paused = Run.start(set, preRoundWarningSeconds = 10, nowMillis = 0).togglePause(12_000)
        assertTrue(paused.clock.isPaused)
        assertEquals(18_000L, paused.snapshot(600_000).phaseRemainingMillis)

        val resumed = paused.togglePause(600_000)
        assertEquals(17_000L, resumed.snapshot(601_000).phaseRemainingMillis)
        val finished = resumed.copy(finished = true)
        assertEquals(finished, finished.togglePause(700_000))
    }

    @Test
    fun `active run is shared until it is cleared`() {
        val run = Run.start(set, preRoundWarningSeconds = 10, nowMillis = 0)
        ActiveRun.begin(run)
        ActiveRun.togglePause(5_000)
        assertTrue(ActiveRun.run.value?.clock?.isPaused == true)
        ActiveRun.finish()
        assertTrue(ActiveRun.run.value?.finished == true)
        ActiveRun.clear()
        assertNull(ActiveRun.run.value)
    }

    @Test
    fun `cue tracker hands out each cue once`() {
        val tracker = CueTracker(listOf(Cue(0, Signal.WARNING), Cue(7_000, Signal.TICK), Cue(10_000, Signal.ROUND_START)))

        assertEquals(Signal.WARNING, tracker.due(0)?.signal)
        assertNull(tracker.due(10))
        assertEquals(7_000L, tracker.nextAtMillis())
        assertEquals(Signal.TICK, tracker.due(7_020)?.signal)
        assertEquals(Signal.ROUND_START, tracker.due(10_000)?.signal)
        assertNull(tracker.due(20_000))
        assertNull(tracker.nextAtMillis())
    }

    @Test
    fun `cue tracker drops cues missed by more than a second`() {
        val tracker = CueTracker(listOf(Cue(7_000, Signal.TICK), Cue(8_000, Signal.TICK), Cue(10_000, Signal.ROUND_START)))

        // Woken up late: only the latest missed cue is still worth playing.
        assertEquals(Signal.ROUND_START, tracker.due(10_500)?.signal)

        val late = CueTracker(listOf(Cue(7_000, Signal.TICK)))
        assertNull(late.due(9_000))
        assertNull(late.nextAtMillis())
    }
}
