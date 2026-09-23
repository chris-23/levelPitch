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
import io.github.cnissler.levelpitch.R
import io.github.cnissler.levelpitch.leveling.ImuReading
import io.github.cnissler.levelpitch.leveling.PhoneOrientation
import io.github.cnissler.levelpitch.leveling.Tilt
import io.github.cnissler.levelpitch.leveling.WindowResult
import io.github.cnissler.levelpitch.leveling.relativeTo
import io.github.cnissler.levelpitch.ui.theme.LevelPitchTheme

/** Zero calibration of the active vehicle (F2), with a raw readout for bench checks. */
@Composable
fun CalibrationScreen(onBack: () -> Unit, onProfiles: () -> Unit) {
    val vm = appViewModel { CalibrationViewModel(it) }
    val state by vm.state.collectAsStateWithLifecycle()
    CalibrationContent(
        state = state,
        onBack = onBack,
        onProfiles = onProfiles,
        onOrientation = vm::selectOrientation,
        onMeasure = vm::measure,
        onSetZero = vm::setZero,
        onClearZero = vm::clearZero,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalibrationContent(
    state: CalibrationUiState,
    onBack: () -> Unit,
    onProfiles: () -> Unit,
    onOrientation: (PhoneOrientation) -> Unit,
    onMeasure: () -> Unit,
    onSetZero: () -> Unit,
    onClearZero: () -> Unit,
) {
    val idle = state.running == null && state.sensorAvailable && state.hasVehicle
    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.calibration)) }, navigationIcon = { BackButton(onBack) })
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (!state.hasVehicle) {
                Text(stringResource(R.string.calibration_needs_vehicle))
                Button(onClick = onProfiles) { Text(stringResource(R.string.profiles)) }
                return@Column
            }
            Text(stringResource(R.string.home_intro), style = MaterialTheme.typography.bodyLarge)

            Text(stringResource(R.string.orientation_label), style = MaterialTheme.typography.labelLarge)
            OrientationPicker(state.orientation, idle, onOrientation, Modifier.fillMaxWidth())

            ZeroSection(state, idle, onSetZero, onClearZero)

            OutlinedButton(onClick = onMeasure, enabled = idle, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.measure_check))
            }

            ResultCard(state)
        }
    }
}

@Composable
private fun ZeroSection(state: CalibrationUiState, idle: Boolean, onSetZero: () -> Unit, onClearZero: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            state.zero?.let { stringResource(R.string.zero_value, displayDeg(it.pitchDeg), displayDeg(it.rollDeg)) }
                ?: stringResource(R.string.zero_none),
        )
        Text(stringResource(R.string.zero_hint), style = MaterialTheme.typography.bodySmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onSetZero, enabled = idle) { Text(stringResource(R.string.set_zero)) }
            if (state.zero != null) {
                TextButton(onClick = onClearZero, enabled = idle) { Text(stringResource(R.string.clear_zero)) }
            }
        }
    }
}

@Composable
private fun ResultCard(state: CalibrationUiState) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            when {
                !state.sensorAvailable -> Text(stringResource(R.string.no_sensor))
                state.running != null -> HoldStill()
                else -> when (val r = state.result) {
                    null -> Unit
                    is WindowResult.Still -> StillResult(r.reading, state)
                    else -> RejectedResult(r)
                }
            }
        }
    }
}

@Composable
private fun StillResult(reading: ImuReading, state: CalibrationUiState) {
    if (state.lastAction == MeasureAction.SET_ZERO) {
        Text(stringResource(R.string.zero_done), style = MaterialTheme.typography.titleMedium)
    } else {
        val tilt = state.zero?.let { reading.tilt.relativeTo(it) } ?: reading.tilt
        TiltLines(tilt)
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
fun HoldStill() {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        CircularProgressIndicator(Modifier.size(24.dp))
        Text(stringResource(R.string.hold_still))
    }
}

/** Message for a measurement that was not accepted; nothing for [WindowResult.Still]. */
@Composable
fun RejectedResult(result: WindowResult) {
    val error = MaterialTheme.colorScheme.error
    when (result) {
        is WindowResult.Still -> Unit
        is WindowResult.Moved -> {
            Text(stringResource(R.string.moved), color = error)
            Quality(result.reading)
        }
        is WindowResult.NotFlat -> Text(stringResource(R.string.not_flat), color = error)
        is WindowResult.TooFewSamples -> Text(stringResource(R.string.too_few_samples, result.count), color = error)
    }
}

@Composable
fun TiltLines(tilt: Tilt) {
    AngleLine(R.string.pitch_value, tilt.pitchDeg, R.string.nose_up, R.string.nose_down)
    AngleLine(R.string.roll_value, tilt.rollDeg, R.string.left_side_up, R.string.right_side_up)
}

@Composable
private fun AngleLine(valueRes: Int, deg: Double, positiveRes: Int, negativeRes: Int) {
    val value = stringResource(valueRes, displayDeg(deg))
    val word = when (direction(deg)) {
        Direction.POSITIVE -> stringResource(positiveRes)
        Direction.NEGATIVE -> stringResource(negativeRes)
        Direction.NONE -> null
    }
    // Direction on its own line: long words ("Querneigung -0,80° rechts höher") would break mid-phrase.
    Column {
        Text(value, style = MaterialTheme.typography.headlineSmall)
        if (word != null) Text(word, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
fun Quality(reading: ImuReading) {
    Text(
        stringResource(R.string.quality, reading.noiseDeg, reading.driftDeg, reading.sampleCount),
        style = MaterialTheme.typography.bodySmall,
    )
}

@Preview(showBackground = true)
@Composable
private fun CalibrationPreview() {
    LevelPitchTheme {
        CalibrationContent(
            state = CalibrationUiState(
                sensorAvailable = true,
                hasVehicle = true,
                orientation = PhoneOrientation.TOP_TO_FRONT,
                zero = Tilt(0.42, -0.13),
                lastAction = MeasureAction.MEASURE,
                result = WindowResult.Still(ImuReading(Tilt(1.65, -0.31), 0.08, 0.01, 201)),
            ),
            onBack = {}, onProfiles = {}, onOrientation = {}, onMeasure = {}, onSetZero = {}, onClearZero = {},
        )
    }
}
