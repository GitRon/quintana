package io.github.gitron.quintana.timer

import io.github.gitron.quintana.TimerSet
import org.junit.Assert.assertEquals
import org.junit.Test

class ScheduleTest {
    private fun set(work: Int, rest: Int, rounds: Int) = TimerSet(name = "Test", workSeconds = work, restSeconds = rest, rounds = rounds)

    private fun Schedule.phases() = segments.map { Triple(it.phase, it.round, it.startMillis / 1000 to it.endMillis / 1000) }

    private fun Schedule.cueSeconds() = cues.map { it.atMillis / 1000 to it.signal }

    /** The run from the acceptance criteria: 3 rounds of 0:20 with 0:15 rest and a 10 s lead-in. */
    @Test
    fun `three rounds with lead-in`() {
        val schedule = Schedule.of(set(work = 20, rest = 15, rounds = 3), preRoundWarningSeconds = 10)

        assertEquals(
            listOf(
                Triple(Phase.LEAD_IN, 1, 0L to 10L),
                Triple(Phase.WORK, 1, 10L to 30L),
                Triple(Phase.REST, 1, 30L to 35L),
                Triple(Phase.LEAD_IN, 2, 35L to 45L),
                Triple(Phase.WORK, 2, 45L to 65L),
                Triple(Phase.REST, 2, 65L to 70L),
                Triple(Phase.LEAD_IN, 3, 70L to 80L),
                Triple(Phase.WORK, 3, 80L to 100L),
            ),
            schedule.phases(),
        )
        assertEquals(100_000L, schedule.totalMillis)
        assertEquals(
            listOf(
                0L to Signal.WARNING, 7L to Signal.TICK, 8L to Signal.TICK, 9L to Signal.TICK,
                10L to Signal.ROUND_START, 30L to Signal.ROUND_END,
                35L to Signal.WARNING, 42L to Signal.TICK, 43L to Signal.TICK, 44L to Signal.TICK,
                45L to Signal.ROUND_START, 65L to Signal.ROUND_END,
                70L to Signal.WARNING, 77L to Signal.TICK, 78L to Signal.TICK, 79L to Signal.TICK,
                80L to Signal.ROUND_START, 100L to Signal.FINISH,
            ),
            schedule.cueSeconds(),
        )
    }

    @Test
    fun `total matches the formula of the specification`() {
        val set = set(work = 120, rest = 60, rounds = 12)
        assertEquals(35 * 60 + 10, set.totalSeconds(10))
        assertEquals(set.totalSeconds(10) * 1000L, Schedule.of(set, 10).totalMillis)
    }

    @Test
    fun `rest shorter than the lead-in is lead-in as a whole`() {
        val schedule = Schedule.of(set(work = 20, rest = 5, rounds = 2), preRoundWarningSeconds = 10)

        assertEquals(
            listOf(
                Triple(Phase.LEAD_IN, 1, 0L to 10L),
                Triple(Phase.WORK, 1, 10L to 30L),
                Triple(Phase.LEAD_IN, 2, 30L to 35L),
                Triple(Phase.WORK, 2, 35L to 55L),
            ),
            schedule.phases(),
        )
        // The round's end and the lead-in fall on the same moment; the gong wins over the warning.
        assertEquals(
            listOf(
                0L to Signal.WARNING, 7L to Signal.TICK, 8L to Signal.TICK, 9L to Signal.TICK,
                10L to Signal.ROUND_START, 30L to Signal.ROUND_END, 32L to Signal.TICK, 33L to Signal.TICK, 34L to Signal.TICK,
                35L to Signal.ROUND_START, 55L to Signal.FINISH,
            ),
            schedule.cueSeconds(),
        )
    }

    @Test
    fun `without rest the rounds follow directly and without lead-in`() {
        val schedule = Schedule.of(set(work = 20, rest = 0, rounds = 3), preRoundWarningSeconds = 10)

        assertEquals(
            listOf(
                Triple(Phase.LEAD_IN, 1, 0L to 10L),
                Triple(Phase.WORK, 1, 10L to 30L),
                Triple(Phase.WORK, 2, 30L to 50L),
                Triple(Phase.WORK, 3, 50L to 70L),
            ),
            schedule.phases(),
        )
        // A new round starting is more important than the old one ending.
        assertEquals(
            listOf(30L to Signal.ROUND_START, 50L to Signal.ROUND_START, 70L to Signal.FINISH),
            schedule.cueSeconds().filter { it.first >= 30 },
        )
    }

    @Test
    fun `without lead-in the run starts with the first round`() {
        val schedule = Schedule.of(set(work = 20, rest = 15, rounds = 2), preRoundWarningSeconds = 0)

        assertEquals(
            listOf(
                Triple(Phase.WORK, 1, 0L to 20L),
                Triple(Phase.REST, 1, 20L to 35L),
                Triple(Phase.WORK, 2, 35L to 55L),
            ),
            schedule.phases(),
        )
        assertEquals(
            listOf(0L to Signal.ROUND_START, 20L to Signal.ROUND_END, 35L to Signal.ROUND_START, 55L to Signal.FINISH),
            schedule.cueSeconds(),
        )
    }

    @Test
    fun `a single round ends with the finish`() {
        val schedule = Schedule.of(set(work = 30, rest = 60, rounds = 1), preRoundWarningSeconds = 0)
        assertEquals(listOf(Triple(Phase.WORK, 1, 0L to 30L)), schedule.phases())
        assertEquals(listOf(0L to Signal.ROUND_START, 30L to Signal.FINISH), schedule.cueSeconds())
    }

    @Test
    fun `lead-in shorter than three seconds ticks only within it`() {
        val schedule = Schedule.of(set(work = 20, rest = 0, rounds = 1), preRoundWarningSeconds = 2)
        assertEquals(
            listOf(0L to Signal.WARNING, 1L to Signal.TICK, 2L to Signal.ROUND_START, 22L to Signal.FINISH),
            schedule.cueSeconds(),
        )
    }

    @Test
    fun `snapshot during work`() {
        val schedule = Schedule.of(set(work = 120, rest = 60, rounds = 12), preRoundWarningSeconds = 10)
        // Round 3 starts after the lead-in, two rounds and two rests: 10 + 2 × 180 = 370 s.
        val snapshot = schedule.snapshot(370_000 + 18_000)

        assertEquals(Phase.WORK, snapshot.phase)
        assertEquals(3, snapshot.round)
        assertEquals(12, snapshot.rounds)
        assertEquals(2, snapshot.completedRounds)
        assertEquals(102_000L, snapshot.phaseRemainingMillis)
        assertEquals(120_000L, snapshot.phaseDurationMillis)
        assertEquals(schedule.totalMillis - 388_000, snapshot.totalRemainingMillis)
        assertEquals(Phase.REST, snapshot.nextPhase)
        assertEquals(0.15f, snapshot.phaseProgress, 0.0001f)
    }

    @Test
    fun `snapshot during rest and the lead-in after it`() {
        val schedule = Schedule.of(set(work = 20, rest = 15, rounds = 3), preRoundWarningSeconds = 10)

        val rest = schedule.snapshot(31_000)
        assertEquals(Phase.REST, rest.phase)
        assertEquals(1, rest.round)
        assertEquals(1, rest.completedRounds)
        assertEquals(Phase.LEAD_IN, rest.nextPhase)

        val leadIn = schedule.snapshot(37_500)
        assertEquals(Phase.LEAD_IN, leadIn.phase)
        assertEquals(2, leadIn.round)
        assertEquals(1, leadIn.completedRounds)
        assertEquals(7_500L, leadIn.phaseRemainingMillis)
    }

    @Test
    fun `snapshot after the end is done`() {
        val schedule = Schedule.of(set(work = 20, rest = 15, rounds = 3), preRoundWarningSeconds = 10)
        val done = schedule.snapshot(100_000)

        assertEquals(Phase.DONE, done.phase)
        assertEquals(3, done.completedRounds)
        assertEquals(0L, done.totalRemainingMillis)
        assertEquals(1f, done.phaseProgress)
    }
}
