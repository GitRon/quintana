package io.github.gitron.quintana.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import io.github.gitron.quintana.Limits
import io.github.gitron.quintana.R
import io.github.gitron.quintana.Steps
import io.github.gitron.quintana.TimerSet
import io.github.gitron.quintana.formatShort
import io.github.gitron.quintana.nameOrDefault

/** Large steps of the steppers while held: 30 s for times, 5 rounds. */
private const val LARGE_STEPS = 6

/**
 * Edits [initial], or creates a new set when it is null. Back drops the changes;
 * an edit never makes a new set.
 */
@Composable
fun EditScreen(
    initial: TimerSet?,
    otherNames: List<String>,
    onBack: () -> Unit,
    onSave: (TimerSet) -> Unit,
    modifier: Modifier = Modifier,
) {
    val start = remember(initial) { initial ?: TimerSet.standard(name = "") }
    var name by rememberSaveable(initial?.id) { mutableStateOf(start.name) }
    var work by rememberSaveable(initial?.id) { mutableIntStateOf(start.workSeconds) }
    var rest by rememberSaveable(initial?.id) { mutableIntStateOf(start.restSeconds) }
    var rounds by rememberSaveable(initial?.id) { mutableIntStateOf(start.rounds) }
    var warning by rememberSaveable(initial?.id) { mutableIntStateOf(start.roundEndWarningSeconds) }
    val draft = start.copy(name = name, workSeconds = work, restSeconds = rest, rounds = rounds, roundEndWarningSeconds = warning)

    Column(
        modifier
            .fillMaxSize()
            .systemBarsPadding()
            .imePadding()
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Header(stringResource(if (initial == null) R.string.new_set else R.string.edit_set), onBack)
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(top = 22.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            NameField(name, onChange = { name = it.take(40) }, placeholder = nameOrDefault("", otherNames))
            Column(
                Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(Kohle),
            ) {
                val workLabel = stringResource(R.string.phase_workout)
                StepperRow(workLabel, Gules, formatShort(work), Limits.work, work) { work = it }
                HorizontalDivider(color = Track)
                StepperRow(stringResource(R.string.phase_rest), Azur, formatShort(rest), Limits.rest, rest) { rest = it }
                HorizontalDivider(color = Track)
                StepperRow(stringResource(R.string.rounds), Pergament, rounds.toString(), Limits.rounds, rounds) { rounds = it }
                HorizontalDivider(color = Track)
                StepperRow(
                    label = stringResource(R.string.phase_closing),
                    detail = stringResource(R.string.warning_detail),
                    color = Or,
                    shown = if (warning == 0) stringResource(R.string.off) else stringResource(R.string.seconds, warning),
                    steps = Limits.roundEndWarning,
                    value = warning,
                ) { warning = it }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.total_duration), fontSize = 15.sp, color = TextSecondary)
                Text(
                    stringResource(R.string.total_minutes, formatShort(draft.totalSeconds())),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Pergament,
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        BigButton(
            stringResource(R.string.save),
            Or,
            onClick = { onSave(draft) },
            modifier = Modifier.fillMaxWidth(),
            height = 64.dp,
            corner = 18.dp,
            textStyle = capitals(24.sp, 0.14f),
        )
    }
}

@Composable
private fun NameField(value: String, onChange: (String) -> Unit, placeholder: String) {
    val focusManager = LocalFocusManager.current
    val label = stringResource(R.string.name)
    val shape = RoundedCornerShape(14.dp)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label.uppercase(), fontSize = 13.sp, letterSpacing = 0.08.em, color = TextSecondary)
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            textStyle = TextStyle(fontFamily = Barlow, fontSize = 18.sp, color = Pergament),
            cursorBrush = SolidColor(Or),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = label },
            decorationBox = { field ->
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(shape)
                        .background(Kohle)
                        .border(1.dp, Linie, shape)
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    // An empty name is saved as the placeholder shows.
                    if (value.isEmpty()) Text(placeholder, fontSize = 18.sp, color = TextSecondary)
                    field()
                }
            },
        )
    }
}

@Composable
private fun StepperRow(
    label: String,
    color: Color,
    shown: String,
    steps: Steps,
    value: Int,
    detail: String? = null,
    onChange: (Int) -> Unit,
) {
    Row(
        Modifier.padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Dot(color)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, fontSize = 18.sp, color = Pergament)
            if (detail != null) Text(detail, fontSize = 13.sp, color = TextSecondary)
        }
        Stepper(
            shown = shown,
            label = label,
            onStep = { count -> onChange(steps.move(value, count)) },
            valueWidth = 70,
            valueSize = 30,
        )
    }
}

/** Minus, value, plus. [onStep] gets the number of steps, negative for minus. */
@Composable
fun Stepper(
    shown: String,
    label: String,
    onStep: (Int) -> Unit,
    valueWidth: Int,
    valueSize: Int,
) {
    StepButton(QuintanaIcons.Minus, stringResource(R.string.less, label), onStep = { large -> onStep(if (large) -LARGE_STEPS else -1) })
    Text(
        shown,
        style = numbers(valueSize.sp),
        color = Pergament,
        textAlign = TextAlign.Center,
        modifier = Modifier.width(valueWidth.dp),
    )
    StepButton(QuintanaIcons.Plus, stringResource(R.string.more, label), onStep = { large -> onStep(if (large) LARGE_STEPS else 1) })
}
