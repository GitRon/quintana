package io.github.gitron.quintana.ui

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.gitron.quintana.Library
import io.github.gitron.quintana.LibraryStore
import io.github.gitron.quintana.TimerSet
import io.github.gitron.quintana.nameOrDefault
import io.github.gitron.quintana.timer.ActiveRun
import io.github.gitron.quintana.timer.Run
import io.github.gitron.quintana.timer.TimerService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

/** The screens besides the timer, which shows whenever a run is active. */
sealed interface Screen {
    data object Home : Screen

    data object Settings : Screen

    /** Edits the set with [setId], or creates a new one when it is null. Back returns to [from]. */
    data class Edit(val setId: String?, val from: Screen) : Screen
}

/** A deleted set and its place, kept for undo. */
data class Deleted(val set: TimerSet, val index: Int)

class QuintanaViewModel(application: Application) : AndroidViewModel(application) {
    private val store = LibraryStore(application)
    private val libraryState = MutableStateFlow<Library?>(null)
    private val screenState = MutableStateFlow<Screen>(Screen.Home)

    /** Null until the library is loaded. */
    val library: StateFlow<Library?> = libraryState.asStateFlow()
    val screen: StateFlow<Screen> = screenState.asStateFlow()
    val run: StateFlow<Run?> = ActiveRun.run

    init {
        viewModelScope.launch {
            libraryState.value = store.load()
            // Every change is written right away; the state flow skips states saved over in the meantime.
            libraryState.filterNotNull().collect { store.save(it) }
        }
    }

    private fun update(change: (Library) -> Library) {
        libraryState.value = libraryState.value?.let(change)
    }

    fun show(screen: Screen) {
        screenState.value = screen
    }

    fun select(setId: String) = update { it.select(setId) }

    /** Saves the edited or new set and shows it in the pager. */
    fun save(draft: TimerSet) {
        update { library ->
            val otherNames = library.sets.filter { it.id != draft.id }.map { it.name }
            val set = draft.copy(name = nameOrDefault(draft.name, otherNames)).clamped()
            library.save(set).select(set.id)
        }
        screenState.value = Screen.Home
    }

    fun delete(set: TimerSet): Deleted? {
        val index = libraryState.value?.sets?.indexOfFirst { it.id == set.id }?.takeIf { it >= 0 } ?: return null
        update { it.delete(set.id) }
        return Deleted(set, index)
    }

    fun restore(deleted: Deleted) = update { it.restore(deleted.set, deleted.index) }

    fun start(set: TimerSet) {
        update { it.select(set.id) }
        TimerService.start(getApplication<Application>(), Run.start(set, SystemClock.elapsedRealtime()))
    }

    fun togglePause() = ActiveRun.togglePause(SystemClock.elapsedRealtime())

    /** Ends the run early, or leaves the finished one; both lead back to the pager. */
    fun closeRun() {
        ActiveRun.clear()
        screenState.value = Screen.Home
    }
}
