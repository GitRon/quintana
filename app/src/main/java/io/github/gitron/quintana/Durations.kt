package io.github.gitron.quintana

/** Whole seconds still to go, rounded up: a countdown shows 0:01 until the last second is over. */
fun secondsLeft(millis: Long): Int = ((millis.coerceAtLeast(0) + 999) / 1000).toInt()

/** "2:00" for set values, "1:02:30" from one hour on. */
fun formatShort(seconds: Int): String {
    val hours = seconds / 3600
    val minutes = seconds % 3600 / 60
    return if (hours > 0) "$hours:${twoDigits(minutes)}:${twoDigits(seconds % 60)}" else "$minutes:${twoDigits(seconds % 60)}"
}

/** "02:00" for the big countdown, "1:02:30" from one hour on. */
fun formatClock(seconds: Int): String = if (seconds >=
    3600
) {
    formatShort(seconds)
} else {
    "${twoDigits(seconds / 60)}:${twoDigits(seconds % 60)}"
}

private fun twoDigits(value: Int) = value.toString().padStart(2, '0')
