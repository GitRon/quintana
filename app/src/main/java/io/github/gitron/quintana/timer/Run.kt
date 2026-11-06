package io.github.gitron.quintana.timer

import io.github.gitron.quintana.TimerSet
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

/**
 * Elapsed time of a run that can be paused, measured on a monotonic clock.
 *
 * The time is computed from when the run was resumed instead of counting
 * ticks, so a late or skipped tick never makes the timer drift.
 */
data class RunClock(val accumulatedMillis: Long = 0, val resumedAtMillis: Long? = null) {
    val isPaused: Boolean get() = resumedAtMillis == null

    fun elapsed(nowMillis: Long): Long = accumulatedMillis + (resumedAtMillis?.let { nowMillis - it } ?: 0)

    fun pause(nowMillis: Long): RunClock = if (isPaused) this else RunClock(elapsed(nowMillis), null)

    fun resume(nowMillis: Long): RunClock = if (isPaused) RunClock(accumulatedMillis, nowMillis) else this

    companion object {
        fun startedAt(nowMillis: Long) = RunClock(0, nowMillis)
    }
}

/** A run of a set: its plan, its clock and whether it is over. */
data class Run(
    val setId: String,
    val setName: String,
    val restSeconds: Int,
    val schedule: Schedule,
    val clock: RunClock,
    /** Set by the service once the finish signal has sounded. */
    val finished: Boolean = false,
    /** Tells two runs of the same set apart. */
    val id: String = UUID.randomUUID().toString(),
) {
    fun snapshot(nowMillis: Long): Snapshot = schedule.snapshot(if (finished) schedule.totalMillis else clock.elapsed(nowMillis))

    fun isFinished(nowMillis: Long): Boolean = finished || clock.elapsed(nowMillis) >= schedule.totalMillis

    /** Pause freezes every phase; a finished run cannot be paused. */
    fun togglePause(nowMillis: Long): Run = when {
        finished -> this
        clock.isPaused -> copy(clock = clock.resume(nowMillis))
        else -> copy(clock = clock.pause(nowMillis))
    }

    companion object {
        fun start(set: TimerSet, preRoundWarningSeconds: Int, nowMillis: Long) = Run(
            setId = set.id,
            setName = set.name,
            restSeconds = set.restSeconds,
            schedule = Schedule.of(set, preRoundWarningSeconds),
            clock = RunClock.startedAt(nowMillis),
        )
    }
}

/** The run in progress, shared by the screen and the service that keeps it going. */
object ActiveRun {
    private val state = MutableStateFlow<Run?>(null)
    val run: StateFlow<Run?> = state.asStateFlow()

    fun begin(run: Run) {
        state.value = run
    }

    fun togglePause(nowMillis: Long) = state.update { it?.togglePause(nowMillis) }

    fun finish() = state.update { it?.copy(finished = true) }

    fun clear() {
        state.value = null
    }
}

/**
 * Hands out the cues of a schedule as the run passes them, each once.
 *
 * When the timer was late by more than [maxLateMillis], for instance because
 * the device was busy, missed cues are dropped instead of played in a burst.
 */
class CueTracker(private val cues: List<Cue>, private val maxLateMillis: Long = 1000) {
    private var next = 0

    /** The cue to play now, if the run reached one since the last call. */
    fun due(elapsedMillis: Long): Cue? {
        var latest: Cue? = null
        while (next < cues.size && cues[next].atMillis <= elapsedMillis) {
            latest = cues[next]
            next++
        }
        return latest?.takeIf { elapsedMillis - it.atMillis <= maxLateMillis }
    }

    /** When the next cue is due, or null after the last one. */
    fun nextAtMillis(): Long? = cues.getOrNull(next)?.atMillis
}
