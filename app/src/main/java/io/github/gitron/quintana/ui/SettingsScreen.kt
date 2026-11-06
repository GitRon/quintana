package io.github.gitron.quintana.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import io.github.gitron.quintana.BuildConfig
import io.github.gitron.quintana.Library
import io.github.gitron.quintana.Limits
import io.github.gitron.quintana.R
import io.github.gitron.quintana.TimerSet
import io.github.gitron.quintana.formatShort

/** The round-end warning for all sets and the list of sets, the only place where sets are deleted. */
@Composable
fun SettingsScreen(
    library: Library,
    onBack: () -> Unit,
    onRoundEndWarningChange: (Int) -> Unit,
    onDelete: (TimerSet) -> Unit,
    onCreate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Header(stringResource(R.string.settings), onBack)
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(top = 22.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            Section(stringResource(R.string.warning_setting)) {
                RoundEndWarningSetting(library.roundEndWarningSeconds, onRoundEndWarningChange)
            }
            Section(stringResource(R.string.my_sets)) {
                if (library.sets.isEmpty()) {
                    Text(
                        stringResource(R.string.no_sets),
                        fontSize = 15.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                } else {
                    Column(
                        Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .background(Kohle),
                    ) {
                        library.sets.forEachIndexed { index, set ->
                            if (index > 0) HorizontalDivider(color = Track)
                            SetRow(set, onDelete = { onDelete(set) })
                        }
                    }
                }
                NewSetButton(onCreate)
            }
            Text(
                stringResource(R.string.version, BuildConfig.VERSION_NAME),
                fontSize = 12.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun Section(label: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label.uppercase(), fontSize = 13.sp, letterSpacing = 0.08.em, color = TextSecondary)
        content()
    }
}

@Composable
private fun RoundEndWarningSetting(seconds: Int, onChange: (Int) -> Unit) {
    val label = stringResource(R.string.warning)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Kohle)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Dot(Or)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(stringResource(R.string.warning_before_end), fontSize = 17.sp, color = Pergament)
            Text(stringResource(R.string.warning_detail), fontSize = 13.sp, color = TextSecondary)
        }
        Stepper(
            shown = if (seconds == 0) stringResource(R.string.off) else stringResource(R.string.seconds, seconds),
            label = label,
            onStep = { count -> onChange(Limits.roundEndWarning.move(seconds, count)) },
            valueWidth = 56,
            valueSize = 28,
        )
    }
}

@Composable
private fun SetRow(set: TimerSet, onDelete: () -> Unit) {
    val deleteLabel = stringResource(R.string.delete_set, set.name)
    Row(
        Modifier.padding(start = 16.dp, end = 10.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                set.name,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = Pergament,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(summary(set), fontSize = 14.sp, color = TextSecondary)
        }
        // Deletes at once; the snackbar offers undo for five seconds.
        Icon(
            QuintanaIcons.Delete,
            contentDescription = null,
            tint = Loeschen,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .clickable(role = Role.Button, onClick = onDelete)
                .semantics { contentDescription = deleteLabel }
                .padding(13.dp),
        )
    }
}

@Composable
private fun NewSetButton(onCreate: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(16.dp))
            .dashedBorder(Linie, 16.dp)
            .clickable(role = Role.Button, onClick = onCreate),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    ) {
        Icon(QuintanaIcons.Plus, contentDescription = null, tint = Or, modifier = Modifier.size(18.dp))
        Text(stringResource(R.string.new_set), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Or)
    }
}

/** "3 Min · 1 Min Pause · 10 Runden" */
@Composable
private fun summary(set: TimerSet): String {
    val rest = if (set.restSeconds == 0) stringResource(R.string.no_rest) else stringResource(R.string.rest_part, duration(set.restSeconds))
    return stringResource(
        R.string.summary,
        duration(set.workSeconds),
        rest,
        pluralStringResource(R.plurals.round_count, set.rounds, set.rounds),
    )
}

@Composable
private fun duration(seconds: Int): String = when {
    seconds < 60 -> stringResource(R.string.seconds, seconds)
    seconds % 60 == 0 -> stringResource(R.string.minutes, (seconds / 60).toString())
    else -> stringResource(R.string.minutes, formatShort(seconds))
}
