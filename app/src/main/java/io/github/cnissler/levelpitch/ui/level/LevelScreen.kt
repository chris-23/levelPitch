package io.github.cnissler.levelpitch.ui.level

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.cnissler.levelpitch.R
import io.github.cnissler.levelpitch.leveling.Caravan
import io.github.cnissler.levelpitch.leveling.PhoneOrientation
import io.github.cnissler.levelpitch.ui.HoldStill
import io.github.cnissler.levelpitch.ui.RejectedResult
import io.github.cnissler.levelpitch.ui.TiltLines
import io.github.cnissler.levelpitch.ui.appViewModel
import io.github.cnissler.levelpitch.ui.profiles.formatDecimal
import kotlin.math.abs

/** Below this, a jockey wheel adjustment isn't worth mentioning. */
private const val MIN_HITCH_ADJUST_MM = 5.0

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LevelScreen(onProfiles: () -> Unit, onCalibration: () -> Unit) {
    val vm = appViewModel { LevelViewModel(it) }
    val state by vm.state.collectAsStateWithLifecycle()
    val ready = state.setup as? Setup.Ready
    var menu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(ready?.vehicleProfile?.name ?: stringResource(R.string.app_name))
                        ready?.let { Text(it.equipmentProfile.name, style = MaterialTheme.typography.bodySmall) }
                    }
                },
                actions = {
                    IconButton(onClick = { menu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.more))
                    }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(text = { Text(stringResource(R.string.new_pitch)) }, enabled = ready != null, onClick = {
                            menu = false
                            vm.newPitch()
                        })
                        DropdownMenuItem(text = { Text(stringResource(R.string.profiles)) }, onClick = {
                            menu = false
                            onProfiles()
                        })
                        DropdownMenuItem(text = { Text(stringResource(R.string.calibration)) }, onClick = {
                            menu = false
                            onCalibration()
                        })
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when (val setup = state.setup) {
                Setup.NoVehicle, Setup.NoEquipment -> {
                    Text(stringResource(R.string.setup_needed))
                    Button(onClick = onProfiles) { Text(stringResource(R.string.profiles)) }
                }
                is Setup.Invalid -> {
                    Text(stringResource(R.string.setup_invalid, setup.message.orEmpty()))
                    Button(onClick = onProfiles) { Text(stringResource(R.string.profiles)) }
                }
                is Setup.Ready -> LevelContent(state, setup, vm, onCalibration)
            }
            Text(
                stringResource(R.string.safety_line),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun LevelContent(state: LevelUiState, setup: Setup.Ready, vm: LevelViewModel, onCalibration: () -> Unit) {
    val plan = state.plan
    if (!state.sensorAvailable) {
        Text(stringResource(R.string.no_sensor), color = MaterialTheme.colorScheme.error)
        return
    }
    if (!state.calibrated) {
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.not_calibrated))
                OutlinedButton(onClick = onCalibration) { Text(stringResource(R.string.calibrate)) }
            }
        }
    }

    Text(stringResource(R.string.placement_hint, stringResource(state.orientation.placement())))
    Button(onClick = vm::measure, enabled = !state.running, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(if (plan == null) R.string.measure else R.string.measure_again))
    }

    MeasurementCard(state, setup)

    val showTargets = plan != null && !plan.isLevel && !plan.stale
    if (plan != null && showTargets) PlanCard(plan, setup, vm::placeRecommended)

    VehicleScene(
        vehicle = setup.vehicle,
        equipment = setup.equipment,
        current = state.wedgeState,
        targets = if (showTargets) plan?.recommendation?.steps else null,
        onSetStep = vm::setStep,
        modifier = Modifier.fillMaxWidth(),
    )
    Text(stringResource(R.string.diagram_hint), style = MaterialTheme.typography.bodySmall)
}

@Composable
private fun MeasurementCard(state: LevelUiState, setup: Setup.Ready) {
    val plan = state.plan
    if (!state.running && state.rejected == null && plan == null) return
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            when {
                state.running -> HoldStill()
                state.rejected != null -> RejectedResult(state.rejected)
                plan != null && plan.stale -> Text(stringResource(R.string.stale), color = MaterialTheme.colorScheme.primary)
                plan != null -> {
                    TiltLines(plan.tilt)
                    if (plan.isLevel) {
                        Text(
                            stringResource(R.string.level_ok, formatDecimal(setup.vehicleProfile.toleranceDeg)),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlanCard(plan: LevelPlan, setup: Setup.Ready, onPlaced: () -> Unit) {
    val rec = plan.recommendation
    val tolerance = setup.vehicleProfile.toleranceDeg
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.plan_title), style = MaterialTheme.typography.titleMedium)
            if (plan.changes.isEmpty()) Text(stringResource(R.string.no_wedge_changes))
            plan.changes.forEach { Text(changeText(it, setup), style = MaterialTheme.typography.bodyLarge) }

            val hitch = rec.hitchAdjustMm
            if (setup.vehicle is Caravan && hitch != null && abs(hitch) >= MIN_HITCH_ADJUST_MM) {
                Text(
                    stringResource(if (hitch > 0) R.string.hitch_raise else R.string.hitch_lower, cm(abs(hitch))),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }

            if (rec.isWithin(tolerance)) {
                Text(stringResource(R.string.residual_ok, formatDecimal(tolerance), rec.residualDeg))
            } else {
                Text(stringResource(R.string.residual_insufficient, rec.residualDeg), color = MaterialTheme.colorScheme.error)
            }

            if (plan.changes.isNotEmpty()) {
                Button(onClick = onPlaced) { Text(stringResource(R.string.wedges_placed)) }
            }
        }
    }
}

@Composable
private fun changeText(change: WedgeChange, setup: Setup.Ready): String {
    val where = groupLabel(change.wheels)
    val mm = setup.equipment.heightMm(change.toStep)
    return when {
        change.toStep == 0 -> stringResource(R.string.change_remove, where)
        change.fromStep == 0 -> stringResource(R.string.change_add, where, change.toStep, cm(mm))
        else -> stringResource(R.string.change_move, where, change.fromStep, change.toStep, cm(mm))
    }
}

private fun PhoneOrientation.placement(): Int = when (this) {
    PhoneOrientation.TOP_TO_FRONT -> R.string.placement_front
    PhoneOrientation.TOP_TO_LEFT -> R.string.placement_left
    PhoneOrientation.TOP_TO_REAR -> R.string.placement_rear
    PhoneOrientation.TOP_TO_RIGHT -> R.string.placement_right
}
