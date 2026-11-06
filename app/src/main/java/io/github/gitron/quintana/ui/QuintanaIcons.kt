package io.github.gitron.quintana.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** Line icons of the mockup, drawn on a 24 grid. Icon() tints them. */
object QuintanaIcons {
    /** Two sliders; the lines stop short of the knobs. */
    val Settings = lines("Settings") {
        moveTo(4f, 7f)
        horizontalLineTo(6.5f)
        moveTo(11.5f, 7f)
        horizontalLineTo(20f)
        moveTo(4f, 17f)
        horizontalLineTo(12.5f)
        moveTo(17.5f, 17f)
        horizontalLineTo(20f)
        circle(9f, 7f, 2.5f)
        circle(15f, 17f, 2.5f)
    }
    val Back = lines("Back") {
        moveTo(15f, 6f)
        lineTo(9f, 12f)
        lineTo(15f, 18f)
    }
    val Edit = lines("Edit") {
        moveTo(4f, 20f)
        horizontalLineTo(8f)
        lineTo(19f, 9f)
        lineTo(15f, 5f)
        lineTo(4f, 16f)
        close()
    }
    val Plus = lines("Plus", width = 2.5f) {
        moveTo(12f, 5f)
        lineTo(12f, 19f)
        moveTo(5f, 12f)
        lineTo(19f, 12f)
    }
    val Minus = lines("Minus", width = 2.5f) {
        moveTo(5f, 12f)
        lineTo(19f, 12f)
    }
    val Delete = lines("Delete") {
        moveTo(4f, 7f)
        horizontalLineTo(20f)
        moveTo(9f, 7f)
        verticalLineTo(4f)
        horizontalLineTo(15f)
        verticalLineTo(7f)
        moveTo(6f, 7f)
        lineTo(7f, 20f)
        horizontalLineTo(17f)
        lineTo(18f, 7f)
    }
    val DisplayOn = lines("DisplayOn") {
        circle(12f, 12f, 4f)
        moveTo(12f, 2f)
        verticalLineTo(4f)
        moveTo(12f, 20f)
        verticalLineTo(22f)
        moveTo(2f, 12f)
        horizontalLineTo(4f)
        moveTo(20f, 12f)
        horizontalLineTo(22f)
    }
    val Play = filled("Play") {
        moveTo(7f, 4f)
        lineTo(20f, 12f)
        lineTo(7f, 20f)
        close()
    }
    val Pause = filled("Pause") {
        rect(6f, 4f, 4f, 16f)
        rect(14f, 4f, 4f, 16f)
    }
    val Stop = filled("Stop") {
        rect(5f, 5f, 14f, 14f)
    }

    private fun lines(name: String, width: Float = 2f, block: PathBuilder.() -> Unit) = ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f)
        .path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = width,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathBuilder = block,
        )
        .build()

    private fun filled(name: String, block: PathBuilder.() -> Unit) = ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f)
        .path(fill = SolidColor(Color.Black), pathBuilder = block)
        .build()

    private fun PathBuilder.circle(x: Float, y: Float, radius: Float) {
        moveTo(x - radius, y)
        arcToRelative(radius, radius, 0f, isMoreThanHalf = true, isPositiveArc = true, dx1 = 2 * radius, dy1 = 0f)
        arcToRelative(radius, radius, 0f, isMoreThanHalf = true, isPositiveArc = true, dx1 = -2 * radius, dy1 = 0f)
        close()
    }

    private fun PathBuilder.rect(x: Float, y: Float, width: Float, height: Float) {
        moveTo(x, y)
        horizontalLineToRelative(width)
        verticalLineToRelative(height)
        horizontalLineToRelative(-width)
        close()
    }
}
