package io.github.gitron.quintana

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class LibraryTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val file: File get() = File(folder.root, "library.json")

    private val sparring = TimerSet(id = "a", name = "Sparring", workSeconds = 120, restSeconds = 60, rounds = 12)
    private val tabata = TimerSet(id = "b", name = "Tabata", workSeconds = 20, restSeconds = 10, rounds = 8)
    private val library = Library(sets = listOf(sparring, tabata), roundEndWarningSeconds = 5, lastSelectedSetId = "b")

    @Test
    fun `first start has the standard set selected and a 10 s warning`() = runTest {
        val first = LibraryStore(file).load()
        assertEquals(listOf("Standard"), first.sets.map { it.name })
        assertEquals(first.sets.single().id, first.lastSelectedSetId)
        assertEquals(10, first.roundEndWarningSeconds)
    }

    @Test
    fun `saved library loads unchanged`() = runTest {
        LibraryStore(file).save(library)
        assertEquals(library, LibraryStore(file).load())
        assertFalse(File(file.path + ".tmp").exists())
    }

    @Test
    fun `unreadable file falls back to the first start`() = runTest {
        file.writeText("{not json")
        assertEquals(listOf("Standard"), LibraryStore(file).load().sets.map { it.name })
    }

    /** Libraries written by released versions must stay readable, or users lose their sets after an update. */
    @Test
    fun `reads libraries written by version 1`() = runTest {
        file.writeText(
            """
            {"version":1,"roundEndWarningSeconds":5,"lastSelectedSetId":"b","sets":[
              {"id":"a","name":"Sparring","workSeconds":120,"restSeconds":60,"rounds":12},
              {"id":"b","name":"Tabata","workSeconds":20,"restSeconds":10,"rounds":8}
            ]}
            """.trimIndent(),
        )
        assertEquals(library, LibraryStore(file).load())
    }

    @Test
    fun `saving an edited set replaces it instead of adding one`() {
        val edited = library.save(sparring.copy(rounds = 10))
        assertEquals(listOf("a", "b"), edited.sets.map { it.id })
        assertEquals(10, edited.set("a")?.rounds)
    }

    @Test
    fun `a new set is added at the end`() {
        val new = TimerSet(id = "c", name = "Set 1", workSeconds = 180, restSeconds = 60, rounds = 10)
        assertEquals(listOf("a", "b", "c"), library.save(new).sets.map { it.id })
    }

    @Test
    fun `undo puts a deleted set back at its place`() {
        val deleted = library.delete("a")
        assertEquals(listOf("b"), deleted.sets.map { it.id })
        assertEquals(listOf("a", "b"), deleted.restore(sparring, 0).sets.map { it.id })
        assertSame(library, library.restore(sparring, 0))
    }

    @Test
    fun `selected index falls back to the first set`() {
        assertEquals(1, library.selectedIndex)
        assertEquals(0, library.delete("b").selectedIndex)
        assertEquals(0, Library(sets = emptyList()).selectedIndex)
    }

    @Test
    fun `lead-in setting stays within its range`() {
        assertEquals(0, library.withRoundEndWarning(-5).roundEndWarningSeconds)
        assertEquals(30, library.withRoundEndWarning(45).roundEndWarningSeconds)
    }
}
