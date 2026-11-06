package io.github.gitron.quintana.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import io.github.gitron.quintana.Library
import io.github.gitron.quintana.R
import io.github.gitron.quintana.TimerSet
import io.github.gitron.quintana.formatClock
import io.github.gitron.quintana.formatShort

/** The pager: one page per set, then the page that makes a new one. Swiping only happens here, never while a run is on. */
@Composable
fun HomeScreen(
    library: Library,
    onOpenSettings: () -> Unit,
    onEdit: (TimerSet) -> Unit,
    onCreate: () -> Unit,
    onStart: (TimerSet) -> Unit,
    onSelect: (TimerSet) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sets = library.sets
    val pagerState = rememberPagerState(initialPage = library.selectedIndex) { sets.size + 1 }
    val currentSets by rememberUpdatedState(sets)
    val currentOnSelect by rememberUpdatedState(onSelect)
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { page -> currentSets.getOrNull(page)?.let(currentOnSelect) }
    }

    Column(
        modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 12.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Wordmark(Modifier.weight(1f))
            CircleButton(QuintanaIcons.Settings, stringResource(R.string.settings), onOpenSettings)
        }
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            pageSpacing = 20.dp,
            key = { page -> sets.getOrNull(page)?.id ?: "new" },
        ) { page ->
            val set = sets.getOrNull(page)
            if (set == null) {
                NewSetPage(onCreate)
            } else {
                SetPage(set, library.preRoundWarningSeconds, onEdit = { onEdit(set) }, onStart = { onStart(set) })
            }
        }
        Spacer(Modifier.height(16.dp))
        PageIndicator(current = pagerState.currentPage, sets = sets.size)
    }
}

@Composable
private fun SetPage(set: TimerSet, preRoundWarningSeconds: Int, onEdit: () -> Unit, onStart: () -> Unit) {
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(20.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                set.name,
                style = TextStyle(fontFamily = BarlowCondensed, fontWeight = FontWeight.Bold, fontSize = 38.sp, letterSpacing = 0.04.em),
                color = Pergament,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            CircleButton(
                QuintanaIcons.Edit,
                stringResource(R.string.edit_set),
                onEdit,
                size = 44.dp,
                iconSize = 18.dp,
                tint = Or,
                framed = false,
            )
        }
        BoxWithConstraints(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(vertical = 20.dp),
            contentAlignment = Alignment.Center,
        ) {
            val ring = minOf(maxWidth, maxHeight, 290.dp)
            Ring(size = ring, stroke = 14.dp, track = Track, color = Gules, remaining = 0f)
            val scale = ring / 290.dp
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.phase_work).uppercase(), style = capitals(fixedSp(22.dp * scale), 0.24f), color = Gules)
                Text(formatClock(set.workSeconds), style = numbers(fixedSp(100.dp * scale)), color = Pergament)
                Text(
                    stringResource(R.string.total, formatShort(set.totalSeconds(preRoundWarningSeconds))),
                    fontSize = 16.sp,
                    color = TextSecondary,
                )
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Tile(formatShort(set.workSeconds), stringResource(R.string.phase_work), Gules, Modifier.weight(1f))
            Tile(formatShort(set.restSeconds), stringResource(R.string.phase_rest), Azur, Modifier.weight(1f))
            Tile(set.rounds.toString(), stringResource(R.string.rounds), Pergament, Modifier.weight(1f))
        }
        Spacer(Modifier.height(24.dp))
        BigButton(
            stringResource(R.string.start),
            Gules,
            onStart,
            Modifier.fillMaxWidth(),
            icon = QuintanaIcons.Play,
        )
    }
}

@Composable
private fun Tile(value: String, label: String, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Kohle)
            .padding(vertical = 14.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, style = numbers(28.sp), color = color)
        Text(label, fontSize = 13.sp, color = TextSecondary)
    }
}

@Composable
private fun NewSetPage(onCreate: () -> Unit) {
    val shape = RoundedCornerShape(28.dp)
    Column(
        Modifier
            .fillMaxSize()
            .padding(top = 20.dp)
            .clip(shape)
            .dashedBorder(Linie, 28.dp)
            .clickable(role = Role.Button, onClick = onCreate),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp, Alignment.CenterVertically),
    ) {
        Box(
            Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(Or),
            contentAlignment = Alignment.Center,
        ) {
            Icon(QuintanaIcons.Plus, contentDescription = null, tint = Eisen, modifier = Modifier.size(40.dp))
        }
        Text(
            stringResource(R.string.new_set),
            style = TextStyle(fontFamily = BarlowCondensed, fontWeight = FontWeight.Bold, fontSize = 32.sp, letterSpacing = 0.04.em),
            color = Pergament,
        )
        Text(
            stringResource(R.string.new_set_hint),
            fontSize = 16.sp,
            lineHeight = 23.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 240.dp),
        )
    }
}

/** One dot per set, the shown one as a wide pill, and a plus for the new set page. */
@Composable
private fun PageIndicator(current: Int, sets: Int) {
    val description = stringResource(R.string.page_position, current + 1, sets + 1)
    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .height(24.dp)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        // Many sets make the dots smaller, so they still fit in one line.
        val unit = minOf(16.dp, maxWidth / (sets + 4))
        val dot = unit / 2
        Row(horizontalArrangement = Arrangement.spacedBy(dot), verticalAlignment = Alignment.CenterVertically) {
            repeat(sets) { page ->
                val active = page == current
                Box(
                    Modifier
                        .size(width = if (active) dot * 3 else dot, height = dot)
                        .clip(CircleShape)
                        .background(if (active) Pergament else Linie),
                )
            }
            val onNewSetPage = current == sets
            Icon(
                QuintanaIcons.Plus,
                contentDescription = null,
                tint = if (onNewSetPage) Pergament else IndicatorInactive,
                modifier = Modifier.size(if (onNewSetPage) 14.dp else 12.dp),
            )
        }
    }
}

/** The ring around the time; [remaining] is the share of the phase still to go, drawn clockwise from the top. */
@Composable
fun Ring(
    size: Dp,
    stroke: Dp,
    track: Color,
    color: Color,
    remaining: Float,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier.size(size)) {
        val width = stroke.toPx()
        val inset = width / 2
        val arcSize = Size(this.size.width - width, this.size.height - width)
        val topLeft = Offset(inset, inset)
        drawArc(track, 0f, 360f, useCenter = false, topLeft = topLeft, size = arcSize, style = Stroke(width))
        if (remaining > 0f) {
            drawArc(
                color,
                -90f,
                360f * remaining.coerceAtMost(1f),
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width, cap = StrokeCap.Round),
            )
        }
    }
}

/** Text that grows with the ring, independent of the font scale, since the ring does not grow with it. */
@Composable
fun fixedSp(size: Dp) = with(LocalDensity.current) { size.toSp() }
