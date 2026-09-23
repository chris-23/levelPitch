package io.github.cnissler.levelpitch.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.cnissler.levelpitch.R
import io.github.cnissler.levelpitch.leveling.ImuReading
import io.github.cnissler.levelpitch.leveling.PhoneOrientation
import io.github.cnissler.levelpitch.leveling.Tilt
import io.github.cnissler.levelpitch.leveling.WindowResult
import io.github.cnissler.levelpitch.leveling.relativeTo
import io.github.cnissler.levelpitch.ui.theme.LevelPitchTheme

/** Simple-mode measurement screen; the level loop (F3) builds on it in M3. */
@Composable
fun HomeScreen(viewModel: MeasureViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    MeasureScreen(
        state = state,
        onOrientation = viewModel::selectOrientation,
        onMeasure = viewModel::measure,
        onSetZero = viewModel::setZero,
        onClearZero = viewModel::clearZero,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeasureScreen(
    state: MeasureUiState,
    onOrientation: (PhoneOrientation) -> Unit,
    onMeasure: () -> Unit,
    onSetZero: () -> Unit,
    onClearZero: () -> Unit,
) {
    val idle = state.running == null && state.sensorAvailable
    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.app_name)) }) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(stringResource(R.string.home_intro), style = MaterialTheme.typography.bodyLarge)

            Text(stringResource(R.string.orientation_label), style = MaterialTheme.typography.labelLarge)
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                PhoneOrientation.entries.forEachIndexed { i, o ->
                    SegmentedButton(
                        selected = o == state.orientation,
                        onClick = { onOrientation(o) },
                        shape = SegmentedButtonDefaults.itemShape(i, PhoneOrientation.entries.size),
                        enabled = idle,
                    ) { Text(stringResource(o.label())) }
                }
            }

            Button(onClick = onMeasure, enabled = idle, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.measure))
            }

            ResultCard(state)

            CalibrationSection(state, idle, onSetZero, onClearZero)

            Text(
                stringResource(R.string.safety_line),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun ResultCard(state: MeasureUiState) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            when {
                !state.sensorAvailable -> Text(stringResource(R.string.no_sensor))
                state.running != null -> Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CircularProgressIndicator(Modifier.size(24.dp))
                    Text(stringResource(R.string.hold_still))
                }
                else -> when (val r = state.result) {
                    null -> Unit
                    is WindowResult.Still -> StillResult(r.reading, state)
                    is WindowResult.Moved -> {
                        Text(stringResource(R.string.moved), color = MaterialTheme.colorScheme.error)
                        Quality(r.reading)
                    }
                    is WindowResult.NotFlat -> Text(stringResource(R.string.not_flat), color = MaterialTheme.colorScheme.error)
                    is WindowResult.TooFewSamples ->
                        Text(stringResource(R.string.too_few_samples, r.count), color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun StillResult(reading: ImuReading, state: MeasureUiState) {
    if (state.lastAction == MeasureAction.SET_ZERO) {
        Text(stringResource(R.string.zero_done), style = MaterialTheme.typography.titleMedium)
    } else {
        val tilt = state.zero?.let { reading.tilt.relativeTo(it) } ?: reading.tilt
        AngleLine(R.string.pitch_value, tilt.pitchDeg, R.string.nose_up, R.string.nose_down)
        AngleLine(R.string.roll_value, tilt.rollDeg, R.string.left_side_up, R.string.right_side_up)
        if (state.zero != null) {
            Text(
                stringResource(R.string.uncorrected, displayDeg(reading.tilt.pitchDeg), displayDeg(reading.tilt.rollDeg)),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
    Quality(reading)
}

@Composable
private fun AngleLine(valueRes: Int, deg: Double, positiveRes: Int, negativeRes: Int) {
    val value = stringResource(valueRes, displayDeg(deg))
    val word = when (direction(deg)) {
        Direction.POSITIVE -> stringResource(positiveRes)
        Direction.NEGATIVE -> stringResource(negativeRes)
        Direction.NONE -> null
    }
    Text(if (word == null) value else "$value  $word", style = MaterialTheme.typography.headlineSmall)
}

@Composable
private fun Quality(reading: ImuReading) {
    Text(
        stringResource(R.string.quality, reading.noiseDeg, reading.driftDeg, reading.sampleCount),
        style = MaterialTheme.typography.bodySmall,
    )
}

@Composable
private fun CalibrationSection(state: MeasureUiState, idle: Boolean, onSetZero: () -> Unit, onClearZero: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.calibration), style = MaterialTheme.typography.titleMedium)
        Text(
            state.zero?.let { stringResource(R.string.zero_value, displayDeg(it.pitchDeg), displayDeg(it.rollDeg)) }
                ?: stringResource(R.string.zero_none),
        )
        Text(stringResource(R.string.zero_hint), style = MaterialTheme.typography.bodySmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onSetZero, enabled = idle) { Text(stringResource(R.string.set_zero)) }
            if (state.zero != null) {
                TextButton(onClick = onClearZero, enabled = idle) { Text(stringResource(R.string.clear_zero)) }
            }
        }
    }
}

private fun PhoneOrientation.label(): Int = when (this) {
    PhoneOrientation.TOP_TO_FRONT -> R.string.orientation_front
    PhoneOrientation.TOP_TO_LEFT -> R.string.orientation_left
    PhoneOrientation.TOP_TO_REAR -> R.string.orientation_rear
    PhoneOrientation.TOP_TO_RIGHT -> R.string.orientation_right
}

@Preview(showBackground = true)
@Composable
private fun MeasureScreenPreview() {
    LevelPitchTheme {
        MeasureScreen(
            state = MeasureUiState(
                sensorAvailable = true,
                orientation = PhoneOrientation.TOP_TO_FRONT,
                zero = Tilt(0.42, -0.13),
                lastAction = MeasureAction.MEASURE,
                result = WindowResult.Still(ImuReading(Tilt(1.65, -0.31), 0.08, 0.01, 201)),
            ),
            onOrientation = {}, onMeasure = {}, onSetZero = {}, onClearZero = {},
        )
    }
}
