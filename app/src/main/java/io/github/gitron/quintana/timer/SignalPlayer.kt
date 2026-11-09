package io.github.gitron.quintana.timer

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.SoundPool
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import io.github.gitron.quintana.R

/**
 * Plays the signals of a run on the media stream, so the system volume sets
 * their loudness. Music playing at the same time gets quieter for a moment
 * instead of stopping.
 */
class SignalPlayer(context: Context) {
    private class Sound(val id: Int, val millis: Long)

    private val attributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    /** Sounds SoundPool has finished loading; it drops a play() of any other without a sound. */
    private val loaded = mutableSetOf<Int>()

    /** A signal due before its sound was loaded, e.g. the first gong right after START. */
    private var waiting: Pair<Sound, Long>? = null

    private val pool = SoundPool.Builder().setMaxStreams(2).setAudioAttributes(attributes).build().apply {
        // Called on the main thread, like play().
        setOnLoadCompleteListener { _, id, status ->
            if (status != 0) return@setOnLoadCompleteListener
            loaded += id
            val (sound, dueAt) = waiting ?: return@setOnLoadCompleteListener
            if (sound.id != id) return@setOnLoadCompleteListener
            waiting = null
            // Played late rather than not at all, unless the moment has clearly passed.
            if (SystemClock.uptimeMillis() - dueAt <= MAX_LATE_MILLIS) start(sound)
        }
    }
    private val sounds = mapOf(
        Signal.FINISH to Sound(pool.load(context, R.raw.gong_finish, 1), 3_000),
        Signal.ROUND_START to Sound(pool.load(context, R.raw.gong_round_start, 1), 2_200),
        Signal.ROUND_END to Sound(pool.load(context, R.raw.gong_round_end, 1), 2_600),
        Signal.WARNING to Sound(pool.load(context, R.raw.warning, 1), 300),
        Signal.TICK to Sound(pool.load(context, R.raw.tick, 1), 60),
    )
    private val audioManager = context.getSystemService(AudioManager::class.java)
    private val focus = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
        .setAudioAttributes(attributes)
        .build()
    private val handler = Handler(Looper.getMainLooper())
    private val abandonFocus = Runnable { audioManager.abandonAudioFocusRequest(focus) }

    fun play(signal: Signal) {
        val sound = sounds.getValue(signal)
        if (sound.id in loaded) {
            start(sound)
        } else {
            waiting = sound to SystemClock.uptimeMillis()
        }
    }

    private fun start(sound: Sound) {
        // During a phone call the focus is refused; the timer then stays silent.
        if (audioManager.requestAudioFocus(focus) != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) return
        pool.play(sound.id, 1f, 1f, 1, 0, 1f)
        handler.removeCallbacks(abandonFocus)
        handler.postDelayed(abandonFocus, sound.millis + 200)
    }

    fun release() {
        handler.removeCallbacks(abandonFocus)
        audioManager.abandonAudioFocusRequest(focus)
        pool.release()
    }

    companion object {
        /** How long the longest signal sounds, so it is not cut off when the run ends. */
        const val LONGEST_MILLIS = 3_000L

        /** How late a signal may still sound when its file was not loaded in time. */
        private const val MAX_LATE_MILLIS = 1_000L
    }
}
