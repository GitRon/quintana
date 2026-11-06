package io.github.gitron.quintana.timer

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Keeps a run going while the app is in the background or the device is locked:
 * plays its signals on time and shows its state in a notification.
 * The service only runs while a run does.
 */
class TimerService : Service() {
    private val scope = MainScope()
    private lateinit var player: SignalPlayer
    private lateinit var notifications: TimerNotifications
    private var wakeLock: PowerManager.WakeLock? = null
    private var watching: Job? = null
    private var stopping: Job? = null
    private var trackedRunId: String? = null
    private var cues = CueTracker(emptyList())

    override fun onCreate() {
        super.onCreate()
        player = SignalPlayer(this)
        notifications = TimerNotifications(this)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val run = ActiveRun.run.value
        // Android expects this soon after every start, even when there is nothing to run.
        ServiceCompat.startForeground(this, TimerNotifications.NOTIFICATION_ID, notifications.build(run, now()), foregroundServiceType())
        when {
            run == null || run.finished -> stop()
            intent?.action == ACTION_TOGGLE -> ActiveRun.togglePause(now())
            else -> watch()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        releaseWakeLock()
        player.release()
        super.onDestroy()
    }

    private fun watch() {
        stopping?.cancel()
        if (watching?.isActive == true) return
        watching = scope.launch {
            ActiveRun.run.collectLatest { run ->
                when {
                    run == null -> stop()
                    // The finish gong still sounds, see finish().
                    run.finished -> Unit
                    else -> follow(run)
                }
            }
        }
    }

    /** Runs until the run is paused, changed or over. */
    private suspend fun follow(run: Run) {
        if (run.id != trackedRunId) {
            trackedRunId = run.id
            cues = CueTracker(run.schedule.cues)
        }
        notifications.update(run, now())
        if (run.clock.isPaused) {
            releaseWakeLock()
            return
        }
        acquireWakeLock(run.schedule.totalMillis - run.clock.elapsed(now()))
        while (true) {
            val now = now()
            val elapsed = run.clock.elapsed(now)
            cues.due(elapsed)?.let { player.play(it.signal) }
            if (elapsed >= run.schedule.totalMillis) {
                finish()
                return
            }
            notifications.update(run, now)
            // Phases and cues fall on whole seconds of the run, and so do the changes of the display.
            val untilNextSecond = 1000 - elapsed % 1000
            val untilCue = cues.nextAtMillis()?.let { it - elapsed } ?: untilNextSecond
            delay(minOf(untilNextSecond, untilCue).coerceAtLeast(1))
        }
    }

    private fun finish() {
        ActiveRun.finish()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopping = scope.launch {
            delay(SignalPlayer.LONGEST_MILLIS)
            releaseWakeLock()
            stopSelf()
        }
    }

    private fun stop() {
        // Leaving the finished run right after it ended still lets its gong sound out.
        if (stopping?.isActive == true) return
        releaseWakeLock()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    /** Keeps the CPU awake while the screen is off, so the signals come on time. */
    private fun acquireWakeLock(remainingMillis: Long) {
        val lock = wakeLock ?: getSystemService(PowerManager::class.java)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "quintana:timer")
            .apply { setReferenceCounted(false) }
            .also { wakeLock = it }
        // The timeout only guards against a lock that is never released.
        lock.acquire(remainingMillis.coerceAtLeast(0) + SignalPlayer.LONGEST_MILLIS + 10_000)
    }

    private fun releaseWakeLock() {
        wakeLock?.takeIf { it.isHeld }?.release()
    }

    private fun foregroundServiceType() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
    } else {
        0
    }

    private fun now() = SystemClock.elapsedRealtime()

    companion object {
        const val ACTION_TOGGLE = "io.github.gitron.quintana.TOGGLE"

        /** Starts [run] and the service that keeps it going. */
        fun start(context: Context, run: Run) {
            ActiveRun.begin(run)
            ContextCompat.startForegroundService(context, Intent(context, TimerService::class.java))
        }
    }
}
