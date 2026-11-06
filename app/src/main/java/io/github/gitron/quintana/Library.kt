package io.github.gitron.quintana

/** Everything the app stores: the sets in pager order and the global settings. */
data class Library(
    val sets: List<TimerSet>,
    val roundEndWarningSeconds: Int = Limits.DEFAULT_ROUND_END_WARNING,
    /** The pager opens on this set when the app starts. */
    val lastSelectedSetId: String? = null,
) {
    fun set(id: String?): TimerSet? = sets.firstOrNull { it.id == id }

    /** Pager page of the last selected set, the first set if it is gone. */
    val selectedIndex: Int get() = sets.indexOfFirst { it.id == lastSelectedSetId }.coerceAtLeast(0)

    /** Replaces the set with the same id, or adds [set] as the last one. */
    fun save(set: TimerSet): Library {
        val index = sets.indexOfFirst { it.id == set.id }
        val updated = if (index < 0) sets + set else sets.toMutableList().apply { this[index] = set }
        return copy(sets = updated)
    }

    fun delete(id: String): Library = copy(sets = sets.filterNot { it.id == id })

    /** Puts a deleted [set] back where it was, so undo restores the pager order. */
    fun restore(set: TimerSet, index: Int): Library {
        if (sets.any { it.id == set.id }) return this
        val updated = sets.toMutableList().apply { add(index.coerceIn(0, size), set) }
        return copy(sets = updated)
    }

    fun select(id: String): Library = copy(lastSelectedSetId = id)

    fun withRoundEndWarning(seconds: Int): Library = copy(roundEndWarningSeconds = Limits.roundEndWarning.clamp(seconds))

    companion object {
        /** What the app starts with on its first launch. */
        fun firstStart(): Library {
            val standard = TimerSet.standard()
            return Library(sets = listOf(standard), lastSelectedSetId = standard.id)
        }
    }
}
