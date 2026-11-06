package io.github.gitron.quintana.timer

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.SoundPool
import android.os.Handler
import android.os.Looper
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
    private val pool = SoundPool.Builder().setMaxStreams(2).setAudioAttributes(attributes).build()
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
        // During a phone call the focus is refused; the timer then stays silent.
        if (audioManager.requestAudioFocus(focus) != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) return
        val sound = sounds.getValue(signal)
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
    }
}
