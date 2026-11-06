package io.github.gitron.quintana.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import io.github.gitron.quintana.R
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Logo and wordmark at the top of the pager. */
@Composable
fun Wordmark(modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Image(painterResource(R.drawable.logo), contentDescription = null, modifier = Modifier.size(30.dp))
        Text(
            stringResource(R.string.app_name).uppercase(),
            style = TextStyle(fontFamily = Cinzel, fontWeight = FontWeight.Bold, fontSize = 17.sp, letterSpacing = 0.1.em),
            color = Pergament,
        )
    }
}

/** A round icon button with a frame, like back and settings. */
@Composable
fun CircleButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    iconSize: Dp = 22.dp,
    tint: Color = Pergament,
    framed: Boolean = true,
) {
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(Kohle)
            .then(if (framed) Modifier.border(1.dp, Linie, CircleShape) else Modifier)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(iconSize))
    }
}

/** Screen header with a back button and a title in Cinzel. */
@Composable
fun Header(title: String, onBack: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        CircleButton(QuintanaIcons.Back, stringResource(R.string.back), onBack)
        Text(title, style = titleStyle(), color = Pergament)
    }
}

/** The large colored buttons: START, PAUSE, SPEICHERN. Text on color is always iron. */
@Composable
fun BigButton(
    text: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    height: Dp = 80.dp,
    corner: Dp = 22.dp,
    textStyle: TextStyle = capitals(30.sp, 0.16f),
) {
    val shape = RoundedCornerShape(corner)
    Row(
        modifier
            .height(height)
            .clip(shape)
            .background(color)
            .clickable(role = Role.Button, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = Eisen, modifier = Modifier.size(24.dp))
            Box(Modifier.size(12.dp))
        }
        Text(text.uppercase(), style = textStyle, color = Eisen)
    }
}

/** A dot in the color of a phase, in front of its value. */
@Composable
fun Dot(color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(color),
    )
}

/**
 * A plus or minus button of a stepper. A tap moves one step; holding it
 * repeats, after a while in large steps, so an hour is reached quickly.
 */
@Composable
fun StepButton(icon: ImageVector, contentDescription: String, onStep: (large: Boolean) -> Unit, modifier: Modifier = Modifier) {
    val currentOnStep by rememberUpdatedState(onStep)
    var pressed by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier
            .size(48.dp)
            .clip(shape)
            .background(if (pressed) Linie else Eisen)
            .border(1.dp, Linie, shape)
            .pointerInput(Unit) {
                coroutineScope {
                    awaitEachGesture {
                        awaitFirstDown()
                        pressed = true
                        var repeated = false
                        val repeat = launch {
                            delay(400)
                            var count = 0
                            while (true) {
                                repeated = true
                                currentOnStep(count >= 15)
                                count++
                                delay(70)
                            }
                        }
                        val up = waitForUpOrCancellation()
                        repeat.cancel()
                        pressed = false
                        if (up != null && !repeated) currentOnStep(false)
                    }
                }
            }
            .semantics {
                role = Role.Button
                this.contentDescription = contentDescription
                onClick {
                    currentOnStep(false)
                    true
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = Pergament, modifier = Modifier.size(20.dp))
    }
}

/** A dashed frame, for the places where something new is made. */
fun Modifier.dashedBorder(color: Color, corner: Dp, width: Dp = 2.dp): Modifier = drawBehind {
    val stroke = width.toPx()
    drawRoundRect(
        color = color,
        topLeft = Offset(stroke / 2, stroke / 2),
        size = Size(size.width - stroke, size.height - stroke),
        cornerRadius = CornerRadius(corner.toPx() - stroke / 2),
        style = Stroke(width = stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 6.dp.toPx()))),
    )
}

/** Keeps the display on while [enabled]. */
@Composable
fun KeepScreenOn(enabled: Boolean) {
    val view = LocalView.current
    DisposableEffect(enabled) {
        view.keepScreenOn = enabled
        onDispose { view.keepScreenOn = false }
    }
}
