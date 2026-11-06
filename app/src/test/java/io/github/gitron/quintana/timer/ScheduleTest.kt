package io.github.gitron.quintana.timer

import io.github.gitron.quintana.TimerSet
import org.junit.Assert.assertEquals
import org.junit.Test

class ScheduleTest {
    private fun set(work: Int, rest: Int, rounds: Int) = TimerSet(name = "Test", workSeconds = work, restSeconds = rest, rounds = rounds)

    private fun Schedule.phases() = segments.map { Triple(it.phase, it.round, it.startMillis / 1000 to it.endMillis / 1000) }

    private fun Schedule.cueSeconds() = cues.map { it.atMillis / 1000 to it.signal }

    /** 3 rounds of 0:30 with 0:15 rest and the warning 10 s before each round ends. */
    @Test
    fun `three rounds with a warning before each end`() {
        val schedule = Schedule.of(set(work = 30, rest = 15, rounds = 3), roundEndWarningSeconds = 10)

        assertEquals(
            listOf(
                Triple(Phase.WORKOUT, 1, 0L to 20L),
                Triple(Phase.CLOSING, 1, 20L to 30L),
                Triple(Phase.REST, 1, 30L to 45L),
                Triple(Phase.WORKOUT, 2, 45L to 65L),
                Triple(Phase.CLOSING, 2, 65L to 75L),
                Triple(Phase.REST, 2, 75L to 90L),
                Triple(Phase.WORKOUT, 3, 90L to 110L),
                Triple(Phase.CLOSING, 3, 110L to 120L),
            ),
            schedule.phases(),
        )
        assertEquals(120_000L, schedule.totalMillis)
        assertEquals(
            listOf(
                0L to Signal.ROUND_START, 20L to Signal.WARNING, 27L to Signal.TICK, 28L to Signal.TICK, 29L to Signal.TICK,
                30L to Signal.ROUND_END,
                45L to Signal.ROUND_START, 65L to Signal.WARNING, 72L to Signal.TICK, 73L to Signal.TICK, 74L to Signal.TICK,
                75L to Signal.ROUND_END,
                90L to Signal.ROUND_START, 110L to Signal.WARNING, 117L to Signal.TICK, 118L to Signal.TICK, 119L to Signal.TICK,
                120L to Signal.FINISH,
            ),
            schedule.cueSeconds(),
        )
    }

    @Test
    fun `the run starts right away and the total has no lead-in`() {
        val set = set(work = 120, rest = 60, rounds = 12)
        assertEquals(12 * 120 + 11 * 60, set.totalSeconds())
        assertEquals(set.totalSeconds() * 1000L, Schedule.of(set, 10).totalMillis)
        assertEquals(Phase.WORKOUT, Schedule.of(set, 10).snapshot(0).phase)
    }

    @Test
    fun `a round shorter than the warning is closing as a whole`() {
        val schedule = Schedule.of(set(work = 5, rest = 10, rounds = 2), roundEndWarningSeconds = 10)

        assertEquals(
            listOf(
                Triple(Phase.CLOSING, 1, 0L to 5L),
                Triple(Phase.REST, 1, 5L to 15L),
                Triple(Phase.CLOSING, 2, 15L to 20L),
            ),
            schedule.phases(),
        )
        // The round's start and the warning fall on the same moment; the gong wins.
        assertEquals(
            listOf(
                0L to Signal.ROUND_START, 2L to Signal.TICK, 3L to Signal.TICK, 4L to Signal.TICK, 5L to Signal.ROUND_END,
                15L to Signal.ROUND_START, 17L to Signal.TICK, 18L to Signal.TICK, 19L to Signal.TICK, 20L to Signal.FINISH,
            ),
            schedule.cueSeconds(),
        )
    }

    @Test
    fun `without rest the next round starts where the last one ends`() {
        val schedule = Schedule.of(set(work = 20, rest = 0, rounds = 2), roundEndWarningSeconds = 10)

        assertEquals(
            listOf(
                Triple(Phase.WORKOUT, 1, 0L to 10L),
                Triple(Phase.CLOSING, 1, 10L to 20L),
                Triple(Phase.WORKOUT, 2, 20L to 30L),
                Triple(Phase.CLOSING, 2, 30L to 40L),
            ),
            schedule.phases(),
        )
        // A new round starting is more important than the old one ending.
        assertEquals(listOf(20L to Signal.ROUND_START), schedule.cueSeconds().filter { it.first == 20L })
    }

    @Test
    fun `without warning there is no closing phase`() {
        val schedule = Schedule.of(set(work = 20, rest = 15, rounds = 2), roundEndWarningSeconds = 0)

        assertEquals(
            listOf(
                Triple(Phase.WORKOUT, 1, 0L to 20L),
                Triple(Phase.REST, 1, 20L to 35L),
                Triple(Phase.WORKOUT, 2, 35L to 55L),
            ),
            schedule.phases(),
        )
        assertEquals(
            listOf(0L to Signal.ROUND_START, 20L to Signal.ROUND_END, 35L to Signal.ROUND_START, 55L to Signal.FINISH),
            schedule.cueSeconds(),
        )
    }

    @Test
    fun `a warning shorter than three seconds ticks only within it`() {
        val schedule = Schedule.of(set(work = 20, rest = 0, rounds = 1), roundEndWarningSeconds = 2)
        assertEquals(
            listOf(0L to Signal.ROUND_START, 18L to Signal.WARNING, 19L to Signal.TICK, 20L to Signal.FINISH),
            schedule.cueSeconds(),
        )
    }

    @Test
    fun `snapshot during the workout`() {
        val schedule = Schedule.of(set(work = 120, rest = 60, rounds = 12), roundEndWarningSeconds = 10)
        // Round 3 starts after two rounds and two rests: 2 × 180 = 360 s.
        val snapshot = schedule.snapshot(360_000 + 18_000)

        assertEquals(Phase.WORKOUT, snapshot.phase)
        assertEquals(3, snapshot.round)
        assertEquals(12, snapshot.rounds)
        assertEquals(2, snapshot.completedRounds)
        // The clock counts down the whole round, closing seconds included.
        assertEquals(102_000L, snapshot.countdownMillis)
        assertEquals(120_000L, snapshot.countdownDurationMillis)
        assertEquals(0.15f, snapshot.countdownProgress, 0.0001f)
        assertEquals(schedule.totalMillis - 378_000, snapshot.totalRemainingMillis)
    }

    @Test
    fun `snapshot in the closing seconds and the rest after them`() {
        val schedule = Schedule.of(set(work = 30, rest = 15, rounds = 3), roundEndWarningSeconds = 10)

        val closing = schedule.snapshot(22_500)
        assertEquals(Phase.CLOSING, closing.phase)
        assertEquals(1, closing.round)
        assertEquals(0, closing.completedRounds)
        assertEquals(7_500L, closing.countdownMillis)
        assertEquals(30_000L, closing.countdownDurationMillis)

        val rest = schedule.snapshot(31_000)
        assertEquals(Phase.REST, rest.phase)
        assertEquals(1, rest.round)
        assertEquals(1, rest.completedRounds)
        assertEquals(14_000L, rest.countdownMillis)
        assertEquals(15_000L, rest.countdownDurationMillis)
    }

    @Test
    fun `a three minute round starts at 3 00`() {
        val schedule = Schedule.of(set(work = 180, rest = 60, rounds = 3), roundEndWarningSeconds = 10)
        assertEquals(Phase.WORKOUT, schedule.snapshot(0).phase)
        assertEquals(180_000L, schedule.snapshot(0).countdownMillis)
        assertEquals(Phase.CLOSING, schedule.snapshot(170_000).phase)
        assertEquals(10_000L, schedule.snapshot(170_000).countdownMillis)
    }

    @Test
    fun `snapshot after the end is done`() {
        val schedule = Schedule.of(set(work = 30, rest = 15, rounds = 3), roundEndWarningSeconds = 10)
        val done = schedule.snapshot(120_000)

        assertEquals(Phase.DONE, done.phase)
        assertEquals(3, done.completedRounds)
        assertEquals(0L, done.totalRemainingMillis)
        assertEquals(1f, done.countdownProgress)
    }
}
