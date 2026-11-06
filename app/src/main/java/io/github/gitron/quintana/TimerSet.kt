package io.github.gitron.quintana

import java.util.UUID

/** One interval workout: [rounds] of [workSeconds] with [restSeconds] between them. */
data class TimerSet(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val workSeconds: Int,
    val restSeconds: Int,
    val rounds: Int,
) {
    /** Duration of a run including the lead-in before the first round. No rest follows the last round. */
    fun totalSeconds(preRoundWarningSeconds: Int): Int = preRoundWarningSeconds + rounds * workSeconds + (rounds - 1) * restSeconds

    /** Values outside the allowed ranges, e.g. from an edited file, moved to the nearest allowed one. */
    fun clamped() = copy(
        workSeconds = Limits.work.clamp(workSeconds),
        restSeconds = Limits.rest.clamp(restSeconds),
        rounds = Limits.rounds.clamp(rounds),
    )

    companion object {
        /** The set the app starts with, and the values a new set starts with. */
        fun standard(name: String = "Standard") = TimerSet(name = name, workSeconds = 180, restSeconds = 60, rounds = 10)
    }
}

/** Range and step of a value set with a stepper. */
data class Steps(val min: Int, val max: Int, val step: Int) {
    fun clamp(value: Int) = value.coerceIn(min, max)

    /**
     * The value [count] steps up (positive) or down (negative), on the grid of [step]
     * and within the range. A value off the grid first moves to the next grid point.
     */
    fun move(value: Int, count: Int): Int {
        val current = clamp(value)
        if (count == 0) return current
        val offGrid = (current - min) % step
        val start = when {
            offGrid == 0 -> current
            count > 0 -> current - offGrid + step
            else -> current - offGrid
        }
        val remaining = if (offGrid == 0) count else count - count.sign()
        return clamp(start + remaining * step)
    }

    private fun Int.sign() = if (this > 0) 1 else -1
}

object Limits {
    val work = Steps(min = 5, max = 3600, step = 5)
    val rest = Steps(min = 0, max = 3600, step = 5)
    val rounds = Steps(min = 1, max = 99, step = 1)
    val preRoundWarning = Steps(min = 0, max = 30, step = 1)
    const val DEFAULT_PRE_ROUND_WARNING = 10
}

/**
 * The name a set is saved under: what the user typed, or "Set N" with the
 * smallest N no other set uses when the field was left empty.
 */
fun nameOrDefault(typed: String, otherNames: Collection<String>): String {
    val trimmed = typed.trim()
    if (trimmed.isNotEmpty()) return trimmed
    val taken = otherNames.toSet()
    return generateSequence(1) { it + 1 }.map { "Set $it" }.first { it !in taken }
}
