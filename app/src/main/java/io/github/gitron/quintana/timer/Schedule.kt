package io.github.gitron.quintana.timer

import io.github.gitron.quintana.TimerSet

enum class Phase { LEAD_IN, WORK, REST, DONE }

/** The sounds of a run, in the order of their priority when two fall on the same moment. */
enum class Signal { FINISH, ROUND_START, ROUND_END, WARNING, TICK }

/**
 * A stretch of a run with one phase.
 *
 * [round] is the round the stretch belongs to: for work the running round,
 * for rest the round just finished, for the lead-in the round about to start.
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
    /** Rounds whose work is over. */
    val completedRounds: Int,
    val phaseRemainingMillis: Long,
    val phaseDurationMillis: Long,
    val totalRemainingMillis: Long,
    /** Phase of the next segment, or [Phase.DONE] after the last one. */
    val nextPhase: Phase,
) {
    /** Share of the current phase that is over, from 0 to 1. */
    val phaseProgress: Float
        get() = if (phaseDurationMillis <= 0) 1f else 1f - phaseRemainingMillis.toFloat() / phaseDurationMillis
}

/**
 * The fixed plan of one run of a set: a lead-in, then work and rest in turn.
 *
 * The last [preRoundWarningSeconds] of every rest are the lead-in to the next
 * round; a shorter rest is lead-in as a whole. Without rest the rounds follow
 * each other directly. No rest follows the last round.
 */
class Schedule(val rounds: Int, val segments: List<Segment>) {
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
                nextPhase = Phase.DONE,
            )
        }
        val segment = segments[index]
        return Snapshot(
            phase = segment.phase,
            round = segment.round,
            rounds = rounds,
            completedRounds = when (segment.phase) {
                Phase.REST -> segment.round
                else -> segment.round - 1
            },
            phaseRemainingMillis = segment.endMillis - elapsedMillis.coerceAtLeast(0),
            phaseDurationMillis = segment.durationMillis,
            totalRemainingMillis = totalMillis - elapsedMillis.coerceAtLeast(0),
            nextPhase = segments.getOrNull(index + 1)?.phase ?: Phase.DONE,
        )
    }

    private fun buildCues(): List<Cue> {
        val cues = mutableListOf<Cue>()
        segments.forEachIndexed { index, segment ->
            when (segment.phase) {
                Phase.LEAD_IN -> {
                    cues += Cue(segment.startMillis, Signal.WARNING)
                    for (secondsBefore in 3 downTo 1) {
                        val at = segment.endMillis - secondsBefore * 1000L
                        if (at > segment.startMillis) cues += Cue(at, Signal.TICK)
                    }
                }
                Phase.WORK -> {
                    cues += Cue(segment.startMillis, Signal.ROUND_START)
                    val last = segments.drop(index + 1).none { it.phase == Phase.WORK }
                    cues += Cue(segment.endMillis, if (last) Signal.FINISH else Signal.ROUND_END)
                }
                Phase.REST, Phase.DONE -> Unit
            }
        }
        // Two sounds at once would blur into each other, so the more important one wins.
        return cues.groupBy { it.atMillis }
            .map { (_, sameTime) -> sameTime.minBy { it.signal.ordinal } }
            .sortedBy { it.atMillis }
    }

    companion object {
        fun of(set: TimerSet, preRoundWarningSeconds: Int): Schedule {
            val warning = preRoundWarningSeconds * 1000L
            val work = set.workSeconds * 1000L
            val rest = set.restSeconds * 1000L
            val segments = mutableListOf<Segment>()
            var time = 0L
            fun add(phase: Phase, round: Int, duration: Long) {
                if (duration <= 0) return
                segments += Segment(phase, round, time, duration)
                time += duration
            }
            add(Phase.LEAD_IN, 1, warning)
            for (round in 1..set.rounds) {
                add(Phase.WORK, round, work)
                if (round < set.rounds) {
                    add(Phase.REST, round, (rest - warning).coerceAtLeast(0))
                    add(Phase.LEAD_IN, round + 1, minOf(rest, warning))
                }
            }
            return Schedule(set.rounds, segments)
        }
    }
}
