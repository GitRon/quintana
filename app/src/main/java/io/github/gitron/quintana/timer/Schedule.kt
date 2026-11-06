package io.github.gitron.quintana.timer

import io.github.gitron.quintana.TimerSet

/** [CLOSING] is the end of a round: its last seconds, counted down in gold. */
enum class Phase { WORKOUT, CLOSING, REST, DONE }

/** The sounds of a run, in the order of their priority when two fall on the same moment. */
enum class Signal { FINISH, ROUND_START, ROUND_END, WARNING, TICK }

/**
 * A stretch of a run with one phase. [round] is the round it belongs to; for
 * rest that is the round just finished.
 */
data class Segment(val phase: Phase, val round: Int, val startMillis: Long, val durationMillis: Long) {
    val endMillis: Long get() = startMillis + durationMillis
}

data class Cue(val atMillis: Long, val signal: Signal)

/** What the timer shows at one moment of a run. */
data class Snapshot(
    val phase: Phase,
    val round: Int,
    val rounds: Int,
    /** Rounds that are over. */
    val completedRounds: Int,
    val phaseRemainingMillis: Long,
    val phaseDurationMillis: Long,
    val totalRemainingMillis: Long,
) {
    /** Share of the current phase that is over, from 0 to 1. */
    val phaseProgress: Float
        get() = if (phaseDurationMillis <= 0) 1f else 1f - phaseRemainingMillis.toFloat() / phaseDurationMillis
}

/**
 * The fixed plan of one run of a set: workout and rest in turn, starting right
 * away with the first round. The last [roundEndWarningSeconds] of every round
 * are its closing seconds; a shorter round is closing as a whole. No rest
 * follows the last round.
 */
class Schedule(val rounds: Int, val segments: List<Segment>, private val roundEndWarningMillis: Long) {
    val totalMillis: Long = segments.lastOrNull()?.endMillis ?: 0

    /** The sounds of the run, at most one per moment. */
    val cues: List<Cue> = buildCues()

    fun snapshot(elapsedMillis: Long): Snapshot {
        val index = segments.indexOfFirst { elapsedMillis < it.endMillis }
        if (index < 0) {
            return Snapshot(
                phase = Phase.DONE,
                round = rounds,
                rounds = rounds,
                completedRounds = rounds,
                phaseRemainingMillis = 0,
                phaseDurationMillis = 0,
                totalRemainingMillis = 0,
            )
        }
        val segment = segments[index]
        return Snapshot(
            phase = segment.phase,
            round = segment.round,
            rounds = rounds,
            completedRounds = if (segment.phase == Phase.REST) segment.round else segment.round - 1,
            phaseRemainingMillis = segment.endMillis - elapsedMillis.coerceAtLeast(0),
            phaseDurationMillis = segment.durationMillis,
            totalRemainingMillis = totalMillis - elapsedMillis.coerceAtLeast(0),
        )
    }

    private fun buildCues(): List<Cue> {
        val cues = mutableListOf<Cue>()
        for (round in 1..rounds) {
            val parts = segments.filter { it.round == round && it.phase != Phase.REST }
            val start = parts.first().startMillis
            val end = parts.last().endMillis
            cues += Cue(start, Signal.ROUND_START)
            if (roundEndWarningMillis > 0) {
                val closing = maxOf(start, end - roundEndWarningMillis)
                cues += Cue(closing, Signal.WARNING)
                for (secondsBefore in 3 downTo 1) {
                    val at = end - secondsBefore * 1000L
                    if (at > closing) cues += Cue(at, Signal.TICK)
                }
            }
            cues += Cue(end, if (round == rounds) Signal.FINISH else Signal.ROUND_END)
        }
        // Two sounds at once would blur into each other, so the more important one wins.
        return cues.groupBy { it.atMillis }
            .map { (_, sameTime) -> sameTime.minBy { it.signal.ordinal } }
            .sortedBy { it.atMillis }
    }

    companion object {
        fun of(set: TimerSet, roundEndWarningSeconds: Int): Schedule {
            val warning = roundEndWarningSeconds * 1000L
            val workout = set.workSeconds * 1000L
            val rest = set.restSeconds * 1000L
            val segments = mutableListOf<Segment>()
            var time = 0L
            fun add(phase: Phase, round: Int, duration: Long) {
                if (duration <= 0) return
                segments += Segment(phase, round, time, duration)
                time += duration
            }
            for (round in 1..set.rounds) {
                add(Phase.WORKOUT, round, (workout - warning).coerceAtLeast(0))
                add(Phase.CLOSING, round, minOf(workout, warning))
                if (round < set.rounds) add(Phase.REST, round, rest)
            }
            return Schedule(set.rounds, segments, warning)
        }
    }
}
