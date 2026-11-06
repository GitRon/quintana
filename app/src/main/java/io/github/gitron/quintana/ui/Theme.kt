package io.github.gitron.quintana.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import io.github.gitron.quintana.R
import io.github.gitron.quintana.timer.Phase

// Charcoal, antique gold and ivory come from the brand kit in brand/.
// Heraldic tinctures for the phases: red is the workout, blue is rest, gold is the end of a round.
val Eisen = Color(0xFF1B1B1B)
val Kohle = Color(0xFF262421)
val Linie = Color(0xFF3B3630)
val Pergament = Color(0xFFF5EAD7)
val TextSecondary = Color(0xFFA89E8E)
val Gules = Color(0xFFD9442E)
val Azur = Color(0xFF4C8FD6)
val Or = Color(0xFFD4AF37)
val ClosingBackground = Color(0xFF221E12)
val Loeschen = Color(0xFFE0705F)

/** Ring track and dividers on iron. */
val Track = Color(0xFF2E2B27)

/** The muted plus of the page indicator when the new set page is not shown. */
val IndicatorInactive = Color(0xFF6E655A)

/** The colors of the timer screen in one phase. */
@Immutable
data class PhaseColors(
    val accent: Color,
    val background: Color,
    val surface: Color,
    val line: Color,
    val track: Color,
    val secondary: Color,
)

private val IronPhase = PhaseColors(Gules, Eisen, Kohle, Linie, Track, TextSecondary)

/** The whole screen turns gold for the last seconds of a round, so it is noticed from the corner of the eye. */
fun phaseColors(phase: Phase): PhaseColors = when (phase) {
    Phase.CLOSING -> PhaseColors(
        accent = Or,
        background = ClosingBackground,
        surface = Color(0xFF2C2614),
        line = Color(0xFF443A22),
        track = Color(0xFF342C19),
        secondary = Color(0xFFB5A98F),
    )
    Phase.WORKOUT -> IronPhase
    Phase.REST -> IronPhase.copy(accent = Azur)
    Phase.DONE -> IronPhase.copy(accent = Pergament)
}

/** Cinzel: only the wordmark and screen titles. */
val Cinzel = FontFamily(Font(R.font.cinzel_bold, FontWeight.Bold))

/** Barlow Condensed: numbers and labels in capitals. */
val BarlowCondensed = FontFamily(
    Font(R.font.barlow_condensed_semibold, FontWeight.SemiBold),
    Font(R.font.barlow_condensed_bold, FontWeight.Bold),
)

/** Barlow: the rest of the interface. */
val Barlow = FontFamily(
    Font(R.font.barlow_regular, FontWeight.Normal),
    Font(R.font.barlow_medium, FontWeight.Medium),
    Font(R.font.barlow_semibold, FontWeight.SemiBold),
)

/** Figures with tabular widths, so a running countdown does not jitter. */
fun numbers(size: TextUnit, weight: FontWeight = FontWeight.Bold) = TextStyle(
    fontFamily = BarlowCondensed,
    fontWeight = weight,
    fontSize = size,
    lineHeight = size,
    fontFeatureSettings = "tnum",
)

/** Labels in capitals with wide letter spacing, like ARBEIT or START. */
fun capitals(size: TextUnit, spacing: Float) = TextStyle(
    fontFamily = BarlowCondensed,
    fontWeight = FontWeight.Bold,
    fontSize = size,
    letterSpacing = spacing.em,
)

fun titleStyle(size: TextUnit = 22.sp) = TextStyle(
    fontFamily = Cinzel,
    fontWeight = FontWeight.Bold,
    fontSize = size,
    letterSpacing = 0.06.em,
)

private val QuintanaTypography = Typography().run {
    fun TextStyle.barlow() = copy(fontFamily = Barlow)
    copy(
        displayLarge = displayLarge.barlow(),
        displayMedium = displayMedium.barlow(),
        displaySmall = displaySmall.barlow(),
        headlineLarge = headlineLarge.barlow(),
        headlineMedium = headlineMedium.barlow(),
        headlineSmall = headlineSmall.barlow(),
        titleLarge = titleLarge.barlow(),
        titleMedium = titleMedium.barlow(),
        titleSmall = titleSmall.barlow(),
        bodyLarge = bodyLarge.barlow(),
        bodyMedium = bodyMedium.barlow(),
        bodySmall = bodySmall.barlow(),
        labelLarge = labelLarge.barlow(),
        labelMedium = labelMedium.barlow(),
        labelSmall = labelSmall.barlow(),
    )
}

private val QuintanaColors = darkColorScheme(
    primary = Or,
    onPrimary = Eisen,
    secondary = Or,
    onSecondary = Eisen,
    background = Eisen,
    onBackground = Pergament,
    surface = Eisen,
    onSurface = Pergament,
    surfaceVariant = Kohle,
    onSurfaceVariant = TextSecondary,
    inverseSurface = Pergament,
    inverseOnSurface = Eisen,
    inversePrimary = Or,
    outline = Linie,
    outlineVariant = Track,
    error = Loeschen,
    onError = Eisen,
)

@Composable
fun QuintanaTheme(content: @Composable () -> Unit) {
    // Dark only, and the brand colors instead of Android's dynamic ones.
    MaterialTheme(colorScheme = QuintanaColors, typography = QuintanaTypography, content = content)
}
