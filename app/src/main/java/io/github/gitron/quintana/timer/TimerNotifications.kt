package io.github.gitron.quintana.timer

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import io.github.gitron.quintana.MainActivity
import io.github.gitron.quintana.R
import io.github.gitron.quintana.formatClock
import io.github.gitron.quintana.formatShort
import io.github.gitron.quintana.secondsLeft

/** The notification of the timer service: phase, remaining time and a pause button. */
class TimerNotifications(private val context: Context) {
    private val manager = context.getSystemService(NotificationManager::class.java)
    private var shown: Pair<String, String>? = null

    init {
        val channel = NotificationChannel(CHANNEL_ID, context.getString(R.string.notification_channel), NotificationManager.IMPORTANCE_LOW)
        channel.setShowBadge(false)
        manager.createNotificationChannel(channel)
    }

    fun build(run: Run?, nowMillis: Long): Notification {
        val (title, text) = run?.let { texts(it, nowMillis) } ?: (context.getString(R.string.app_name) to "")
        val open = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(ContextCompat.getColor(context, R.color.or))
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(open)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setShowWhen(false)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
        if (run != null && !run.finished) {
            val toggle = PendingIntent.getService(
                context,
                1,
                Intent(context, TimerService::class.java).setAction(TimerService.ACTION_TOGGLE),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            val label = if (run.clock.isPaused) R.string.resume else R.string.pause
            builder.addAction(0, context.getString(label), toggle)
        }
        return builder.build()
    }

    /** Shows the current state if it differs from what the notification already says. */
    fun update(run: Run, nowMillis: Long) {
        // Without the permission Android hides the notification anyway.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val texts = texts(run, nowMillis)
        if (texts == shown) return
        shown = texts
        manager.notify(NOTIFICATION_ID, build(run, nowMillis))
    }

    private fun texts(run: Run, nowMillis: Long): Pair<String, String> {
        val snapshot = run.snapshot(nowMillis)
        val phase = context.getString(
            when (snapshot.phase) {
                Phase.WORKOUT -> R.string.phase_workout
                Phase.CLOSING -> R.string.phase_closing
                Phase.REST -> R.string.phase_rest
                Phase.DONE -> R.string.phase_done
            },
        )
        val remaining = formatClock(secondsLeft(snapshot.phaseRemainingMillis))
        val title = if (run.clock.isPaused) {
            context.getString(R.string.notification_paused, phase, remaining)
        } else {
            context.getString(R.string.notification_title, phase, remaining)
        }
        val text = context.getString(
            R.string.notification_text,
            run.setName,
            snapshot.round,
            snapshot.rounds,
            formatShort(secondsLeft(snapshot.totalRemainingMillis)),
        )
        return title to text
    }

    companion object {
        const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "timer"
    }
}
