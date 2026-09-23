package io.github.cnissler.levelpitch.ui.profiles

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.cnissler.levelpitch.R
import io.github.cnissler.levelpitch.profiles.EquipmentKind
import io.github.cnissler.levelpitch.profiles.EquipmentProfile
import io.github.cnissler.levelpitch.profiles.VehicleProfile
import io.github.cnissler.levelpitch.profiles.VehicleType
import io.github.cnissler.levelpitch.ui.BackButton
import io.github.cnissler.levelpitch.ui.appViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfilesScreen(
    onBack: () -> Unit,
    onEditVehicle: (id: String?) -> Unit,
    onEditEquipment: (id: String?) -> Unit,
) {
    val vm = appViewModel { ProfilesViewModel(it) }
    val data by vm.data.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.profiles)) }, navigationIcon = { BackButton(onBack) })
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            Section(R.string.vehicles)
            if (data.vehicles.isEmpty()) Hint(R.string.no_vehicles)
            data.vehicles.forEach { v ->
                ProfileRow(
                    name = v.name,
                    summary = vehicleSummary(v),
                    selected = v.id == data.activeVehicleId,
                    onSelect = { vm.selectVehicle(v.id) },
                    onEdit = { onEditVehicle(v.id) },
                )
            }
            AddButton(R.string.add_vehicle) { onEditVehicle(null) }

            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            Section(R.string.wedge_sets)
            if (data.equipment.isEmpty()) Hint(R.string.no_wedge_sets)
            data.equipment.forEach { e ->
                ProfileRow(
                    name = e.name,
                    summary = equipmentSummary(e),
                    selected = e.id == data.activeEquipmentId,
                    onSelect = { vm.selectEquipment(e.id) },
                    onEdit = { onEditEquipment(e.id) },
                )
            }
            AddButton(R.string.add_wedge_set) { onEditEquipment(null) }
        }
    }
}

@Composable
private fun Section(title: Int) {
    Text(
        stringResource(title),
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 4.dp),
    )
}

@Composable
private fun Hint(text: Int) {
    Text(stringResource(text), modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
}

@Composable
private fun ProfileRow(name: String, summary: String, selected: Boolean, onSelect: () -> Unit, onEdit: () -> Unit) {
    ListItem(
        headlineContent = { Text(name) },
        supportingContent = { Text(summary) },
        leadingContent = { RadioButton(selected = selected, onClick = onSelect) },
        trailingContent = {
            IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.edit)) }
        },
        modifier = Modifier.clickable(onClick = onSelect),
    )
}

@Composable
private fun AddButton(text: Int, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.padding(horizontal = 8.dp)) {
        Icon(Icons.Default.Add, contentDescription = null)
        Text(stringResource(text), modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun vehicleSummary(v: VehicleProfile): String {
    fun cm(mm: Double?) = mm?.let { formatDecimal(it / 10) } ?: "?"
    return when (v.type) {
        VehicleType.MOTORHOME_2AXLE -> stringResource(R.string.summary_motorhome, cm(v.wheelbaseMm), cm(v.trackMm))
        VehicleType.CARAVAN_SINGLE -> stringResource(R.string.summary_caravan_single, cm(v.trackMm), cm(v.hitchToAxleMm))
        VehicleType.CARAVAN_TANDEM ->
            stringResource(R.string.summary_caravan_tandem, cm(v.trackMm), cm(v.tandemSpacingMm), cm(v.hitchToAxleMm))
    }
}

@Composable
private fun equipmentSummary(e: EquipmentProfile): String = when (e.kind) {
    EquipmentKind.STEPPED ->
        stringResource(R.string.summary_wedges, e.stepHeightsMm.joinToString(" / ") { formatDecimal(it / 10) }, e.wedgesOwned)
    EquipmentKind.CONTINUOUS ->
        stringResource(R.string.summary_continuous, formatDecimal((e.maxLiftMm ?: 0.0) / 10), e.wedgesOwned)
}
