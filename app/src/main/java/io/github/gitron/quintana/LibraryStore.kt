package io.github.gitron.quintana

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File

/** Sets and settings in one JSON file in the app's private storage. */
class LibraryStore(private val file: File) {
    constructor(context: Context) : this(File(context.filesDir, "library.json"))

    private val mutex = Mutex()

    /** The stored library; on the first start, or if the file cannot be read, the default one. */
    suspend fun load(): Library = mutex.withLock {
        withContext(Dispatchers.IO) {
            if (!file.isFile) return@withContext Library.firstStart()
            try {
                JSONObject(file.readText()).toLibrary()
            } catch (e: JSONException) {
                Library.firstStart()
            }
        }
    }

    /** Writes to a temporary file first, so a crash cannot leave a half-written library. */
    suspend fun save(library: Library) = mutex.withLock {
        withContext(Dispatchers.IO) {
            val json = JSONObject()
                .put("version", 1)
                .put("preRoundWarningSeconds", library.preRoundWarningSeconds)
                .put("lastSelectedSetId", library.lastSelectedSetId ?: JSONObject.NULL)
                .put("sets", JSONArray(library.sets.map { it.toJson() }))
            val temporary = File(file.path + ".tmp")
            temporary.writeText(json.toString())
            temporary.renameTo(file)
        }
    }

    private fun TimerSet.toJson() = JSONObject()
        .put("id", id)
        .put("name", name)
        .put("workSeconds", workSeconds)
        .put("restSeconds", restSeconds)
        .put("rounds", rounds)

    private fun JSONObject.toLibrary(): Library {
        val sets = getJSONArray("sets")
        return Library(
            sets = (0 until sets.length()).map { sets.getJSONObject(it).toSet() },
            preRoundWarningSeconds = Limits.preRoundWarning.clamp(optInt("preRoundWarningSeconds", Limits.DEFAULT_PRE_ROUND_WARNING)),
            lastSelectedSetId = if (isNull("lastSelectedSetId")) null else optString("lastSelectedSetId"),
        )
    }

    private fun JSONObject.toSet() = TimerSet(
        id = getString("id"),
        name = getString("name"),
        workSeconds = getInt("workSeconds"),
        restSeconds = getInt("restSeconds"),
        rounds = getInt("rounds"),
    ).clamped()
}
