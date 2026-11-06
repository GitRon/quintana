package io.github.gitron.quintana.ui

import android.os.SystemClock
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.gitron.quintana.R
import io.github.gitron.quintana.formatClock
import io.github.gitron.quintana.formatShort
import io.github.gitron.quintana.secondsLeft
import io.github.gitron.quintana.timer.Phase
import io.github.gitron.quintana.timer.Run
import io.github.gitron.quintana.timer.Snapshot
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** How long the reset button has to be held while the timer runs, so a gloved hand does not reset by accident. */
private const val HOLD_TO_RESET_MILLIS = 1000

@Composable
fun TimerScreen(run: Run, onTogglePause: () -> Unit, onClose: () -> Unit, modifier: Modifier = Modifier) {
    // The display follows the clock every frame; the time comes from the run's monotonic clock, not from counting frames.
    var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    LaunchedEffect(run) {
        now = SystemClock.elapsedRealtime()
        while (!run.clock.isPaused && !run.finished) {
            withFrameMillis { }
            now = SystemClock.elapsedRealtime()
        }
    }
    val snapshot = run.snapshot(now)
    val done = snapshot.phase == Phase.DONE
    val colors = phaseColors(snapshot.phase)
    val background by animateColorAsState(colors.background, label = "background")
    var showResetHint by remember { mutableStateOf(false) }
    LaunchedEffect(showResetHint) {
        if (showResetHint) {
            delay(2500)
            showResetHint = false
        }
    }

    KeepScreenOn(!run.finished)

    Column(
        modifier
            .fillMaxSize()
            .background(background)
            .systemBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(48.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                run.setName,
                style = numbers(22.sp),
                color = Pergament,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (!run.finished) {
                Icon(QuintanaIcons.DisplayOn, contentDescription = null, tint = colors.secondary, modifier = Modifier.size(16.dp))
                Spacer(Modifier.size(6.dp))
                Text(stringResource(R.string.display_on), fontSize = 13.sp, color = colors.secondary)
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            phaseLabel(snapshot.phase),
            style = capitals(30.sp, if (snapshot.phase == Phase.CLOSING) 0.2f else 0.28f),
            color = colors.accent,
        )
        BoxWithConstraints(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            val ring = minOf(maxWidth, maxHeight, 310.dp)
            val remaining = if (done) 1f else 1f - snapshot.phaseProgress
            Ring(size = ring, stroke = 16.dp, track = colors.track, color = colors.accent, remaining = remaining)
            val scale = ring / 310.dp
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                val seconds = secondsLeft(snapshot.phaseRemainingMillis)
                // The closing seconds count down as a bare number, readable from across the room.
                if (snapshot.phase == Phase.CLOSING) {
                    Text(seconds.toString(), style = numbers(fixedSp(150.dp * scale)), color = colors.accent)
                } else {
                    Text(formatClock(seconds), style = numbers(fixedSp(108.dp * scale)), color = Pergament)
                }
                Text(subline(run, snapshot), fontSize = 16.sp, color = colors.secondary)
            }
        }
        RoundProgress(snapshot, colors)
        Spacer(Modifier.height(16.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (showResetHint) {
                Text(stringResource(R.string.reset_hint), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = colors.accent)
            }
        }
        Spacer(Modifier.height(8.dp))
        if (done) {
            BigButton(stringResource(R.string.back), Pergament, onClose, Modifier.fillMaxWidth())
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ResetButton(colors, paused = run.clock.isPaused, onReset = onClose, onTapWhileRunning = { showResetHint = true })
                BigButton(
                    stringResource(if (run.clock.isPaused) R.string.resume else R.string.pause),
                    colors.accent,
                    onTogglePause,
                    Modifier.weight(1f),
                    icon = if (run.clock.isPaused) QuintanaIcons.Play else QuintanaIcons.Pause,
                )
            }
        }
    }
}

@Composable
private fun phaseLabel(phase: Phase): String = stringResource(
    when (phase) {
        Phase.WORKOUT -> R.string.phase_workout
        Phase.CLOSING -> R.string.phase_closing
        Phase.REST -> R.string.phase_rest
        Phase.DONE -> R.string.phase_done
    },
).uppercase()

/** The line under the time: what comes next. */
@Composable
private fun subline(run: Run, snapshot: Snapshot): String = when {
    snapshot.phase == Phase.DONE -> pluralStringResource(R.plurals.rounds_done, snapshot.rounds, snapshot.rounds)
    run.clock.isPaused -> stringResource(R.string.paused)
    snapshot.phase == Phase.REST -> stringResource(R.string.then_round, snapshot.round + 1)
    snapshot.round == snapshot.rounds -> stringResource(R.string.last_round)
    run.restSeconds == 0 -> stringResource(R.string.then_round, snapshot.round + 1)
    else -> stringResource(R.string.then_rest, formatShort(run.restSeconds))
}

/** "Runde 3 / 12", the time left of the whole set and one segment per round. */
@Composable
private fun RoundProgress(snapshot: Snapshot, colors: PhaseColors) {
    val (label, detail) = when {
        snapshot.phase == Phase.DONE -> stringResource(R.string.progress_done) to stringResource(R.string.progress_of, snapshot.rounds)
        snapshot.phase == Phase.REST ->
            stringResource(R.string.phase_rest) to stringResource(R.string.progress_after, snapshot.completedRounds, snapshot.rounds)
        else -> stringResource(R.string.progress_round, snapshot.round) to stringResource(R.string.progress_of, snapshot.rounds)
    }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            Text(
                buildAnnotatedString {
                    append(label)
                    append(" ")
                    withStyle(SpanStyle(color = colors.secondary)) { append(detail) }
                },
                style = numbers(24.sp, FontWeight.SemiBold),
                color = Pergament,
                modifier = Modifier.weight(1f),
            )
            if (snapshot.phase != Phase.DONE) {
                Text(
                    stringResource(R.string.remaining_total, formatShort(secondsLeft(snapshot.totalRemainingMillis))),
                    fontSize = 14.sp,
                    color = colors.secondary,
                )
            }
        }
        Segments(snapshot, colors)
    }
}

/** Done rounds light, the current one in the phase color, the coming ones dark. */
@Composable
private fun Segments(snapshot: Snapshot, colors: PhaseColors) {
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(8.dp),
    ) {
        val count = snapshot.rounds
        val gap = minOf(4.dp.toPx(), size.width / count * 0.3f)
        val width = (size.width - gap * (count - 1)) / count
        val radius = CornerRadius(minOf(size.height, width) / 2)
        for (index in 0 until count) {
            val color = when {
                index < snapshot.completedRounds -> colors.secondary
                index == snapshot.completedRounds -> colors.accent
                else -> colors.line
            }
            drawRoundRect(
                color = color,
                topLeft = Offset(index * (width + gap), 0f),
                size = Size(width, size.height),
                cornerRadius = radius,
            )
        }
    }
}

/**
 * Resets the run: back to the pager, ready for START. While the timer runs it has to
 * be held for a second, filling up meanwhile, and a tap only explains that; once
 * paused, a tap resets. Screen readers get the reset as a long-press action.
 */
@Composable
private fun ResetButton(colors: PhaseColors, paused: Boolean, onReset: () -> Unit, onTapWhileRunning: () -> Unit) {
    val currentOnReset by rememberUpdatedState(onReset)
    val currentOnTap by rememberUpdatedState(onTapWhileRunning)
    val currentPaused by rememberUpdatedState(paused)
    val progress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val resetLabel = stringResource(R.string.reset)
    val hint = stringResource(R.string.reset_hint)
    val shape = RoundedCornerShape(22.dp)
    val fill = colors.accent.copy(alpha = 0.45f)
    Box(
        Modifier
            .size(80.dp)
            .clip(shape)
            .background(colors.surface)
            .border(1.dp, colors.line, shape)
            .drawBehind { drawRect(fill, size = Size(size.width * progress.value, size.height)) }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        if (currentPaused) {
                            if (tryAwaitRelease()) currentOnReset()
                        } else {
                            val hold = scope.launch {
                                progress.snapTo(0f)
                                progress.animateTo(1f, tween(HOLD_TO_RESET_MILLIS, easing = LinearEasing))
                                currentOnReset()
                            }
                            tryAwaitRelease()
                            if (hold.isActive) {
                                hold.cancel()
                                currentOnTap()
                                scope.launch { progress.animateTo(0f, tween(150)) }
                            }
                        }
                    },
                )
            }
            .semantics {
                role = Role.Button
                contentDescription = if (paused) resetLabel else "$resetLabel. $hint"
                onClick {
                    if (currentPaused) currentOnReset() else currentOnTap()
                    true
                }
                onLongClick(resetLabel) {
                    currentOnReset()
                    true
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(QuintanaIcons.Reset, contentDescription = null, tint = Pergament, modifier = Modifier.size(28.dp))
    }
}
