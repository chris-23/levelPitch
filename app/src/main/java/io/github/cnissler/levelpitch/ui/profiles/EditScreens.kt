package io.github.cnissler.levelpitch.ui.profiles

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.cnissler.levelpitch.R
import io.github.cnissler.levelpitch.profiles.CaravanSize
import io.github.cnissler.levelpitch.profiles.EquipmentKind
import io.github.cnissler.levelpitch.profiles.VehicleType
import io.github.cnissler.levelpitch.profiles.caravanSizes
import io.github.cnissler.levelpitch.profiles.vehiclePresets
import io.github.cnissler.levelpitch.profiles.wedgePresets
import io.github.cnissler.levelpitch.ui.BackButton
import io.github.cnissler.levelpitch.ui.ConfirmDeleteDialog
import io.github.cnissler.levelpitch.ui.OrientationPicker
import io.github.cnissler.levelpitch.ui.appViewModel
import io.github.cnissler.levelpitch.ui.displayDecimal

@Composable
fun VehicleEditScreen(id: String?, onDone: () -> Unit) {
    val vm = appViewModel(key = "vehicle-$id") { VehicleEditViewModel(it, id) }
    val form = vm.form
    val errors = if (vm.showErrors) form.errors() else emptySet()
    EditorScaffold(
        title = if (vm.isNew) R.string.new_vehicle else R.string.edit_vehicle,
        onBack = onDone,
        onSave = { if (vm.save()) onDone() },
        onDelete = if (vm.isNew) null else ({ vm.delete(); onDone() }),
        deleteTitle = R.string.delete_vehicle_title,
        deleteText = R.string.delete_vehicle_text,
    ) {
        TextInput(R.string.field_name, form.name, VehicleField.NAME in errors, R.string.field_required) {
            vm.form = form.copy(name = it)
        }

        Text(stringResource(R.string.field_type), style = MaterialTheme.typography.labelLarge)
        Column {
            VehicleType.entries.forEach { t ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .selectable(selected = form.type == t, role = Role.RadioButton) { vm.form = form.withType(t) },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = form.type == t, onClick = null)
                    Text(stringResource(t.label()), modifier = Modifier.padding(start = 8.dp, top = 8.dp, bottom = 8.dp))
                }
            }
        }

        if (form.type == VehicleType.MOTORHOME_2AXLE) {
            BasePicker(form) { vm.form = it }
        } else {
            CaravanSizePicker(form) { vm.form = it }
        }

        if (VehicleField.WHEELBASE in form.fields) {
            NumberInput(R.string.field_wheelbase, form.wheelbaseCm, VehicleField.WHEELBASE in errors, R.string.field_positive_number) {
                vm.form = form.copy(wheelbaseCm = it)
            }
        }
        NumberInput(
            R.string.field_track, form.trackCm, VehicleField.TRACK in errors, R.string.field_positive_number,
            help = R.string.field_track_help,
        ) { vm.form = form.copy(trackCm = it) }
        if (VehicleField.TANDEM in form.fields) {
            NumberInput(R.string.field_tandem, form.tandemSpacingCm, VehicleField.TANDEM in errors, R.string.field_positive_number) {
                vm.form = form.copy(tandemSpacingCm = it)
            }
        }
        if (VehicleField.HITCH in form.fields) {
            NumberInput(
                R.string.field_hitch, form.hitchToAxleCm, VehicleField.HITCH in errors, R.string.field_positive_number,
                help = R.string.field_hitch_help,
            ) { vm.form = form.copy(hitchToAxleCm = it) }
        }

        Text(stringResource(R.string.orientation_label), style = MaterialTheme.typography.labelLarge)
        OrientationPicker(form.orientation, enabled = true, onSelect = { vm.form = form.copy(orientation = it) }, Modifier.fillMaxWidth())

        NumberInput(
            R.string.field_tolerance, form.toleranceDeg, VehicleField.TOLERANCE in errors, R.string.field_tolerance_invalid,
            help = R.string.field_tolerance_help,
        ) { vm.form = form.copy(toleranceDeg = it) }
    }
}

@Composable
fun EquipmentEditScreen(id: String?, onDone: () -> Unit) {
    val vm = appViewModel(key = "equipment-$id") { EquipmentEditViewModel(it, id) }
    val form = vm.form
    val errors = if (vm.showErrors) form.errors() else emptySet()
    EditorScaffold(
        title = if (vm.isNew) R.string.new_wedge_set else R.string.edit_wedge_set,
        onBack = onDone,
        onSave = { if (vm.save()) onDone() },
        onDelete = if (vm.isNew) null else ({ vm.delete(); onDone() }),
        deleteTitle = R.string.delete_wedge_set_title,
        deleteText = R.string.delete_wedge_set_text,
    ) {
        TextInput(R.string.field_name, form.name, EquipmentField.NAME in errors, R.string.field_required) {
            vm.form = form.copy(name = it)
        }

        Text(stringResource(R.string.equipment_kind), style = MaterialTheme.typography.labelLarge)
        Column {
            EquipmentKind.entries.forEach { k ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .selectable(selected = form.kind == k, role = Role.RadioButton) { vm.form = form.copy(kind = k) },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = form.kind == k, onClick = null)
                    Text(
                        stringResource(if (k == EquipmentKind.STEPPED) R.string.kind_stepped else R.string.kind_continuous),
                        modifier = Modifier.padding(start = 8.dp, top = 8.dp, bottom = 8.dp),
                    )
                }
            }
        }

        if (form.kind == EquipmentKind.CONTINUOUS) {
            NumberInput(
                R.string.field_max_lift, form.maxLiftCm, EquipmentField.MAX_LIFT in errors, R.string.max_lift_invalid,
                help = R.string.field_max_lift_help,
            ) { vm.form = form.copy(maxLiftCm = it) }
        } else {
            SteppedFields(form, errors) { vm.form = it }
        }

        TextInput(
            if (form.kind == EquipmentKind.CONTINUOUS) R.string.field_devices_owned else R.string.field_wedges_owned,
            form.wedgesOwned, EquipmentField.WEDGES in errors, R.string.wedges_owned_invalid,
            keyboardType = KeyboardType.Number,
        ) { vm.form = form.copy(wedgesOwned = it) }
    }
}

/** Wedge model picker and the list of step heights. */
@Composable
private fun SteppedFields(form: EquipmentForm, errors: Set<EquipmentField>, onChange: (EquipmentForm) -> Unit) {
    PresetDropdown(
        title = R.string.wedge_model,
        selected = wedgePresets.find { it.id == form.presetId }?.name,
        placeholder = R.string.wedge_model_choose,
        options = wedgePresets.map { it.name },
        onPick = { onChange(form.withPreset(wedgePresets[it])) },
        onCustom = { onChange(form.copy(presetId = null)) },
    )

    Text(stringResource(R.string.steps_help), style = MaterialTheme.typography.bodyMedium)
    form.stepsCm.forEachIndexed { i, step ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = step,
                onValueChange = { v -> onChange(form.copy(stepsCm = form.stepsCm.toMutableList().also { it[i] = v })) },
                label = { Text(stringResource(R.string.field_step, i + 1)) },
                isError = EquipmentField.STEPS in errors,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
            )
            if (form.stepsCm.size > 1) {
                IconButton(onClick = { onChange(form.copy(stepsCm = form.stepsCm.filterIndexed { j, _ -> j != i })) }) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.remove_step))
                }
            }
        }
    }
    if (EquipmentField.STEPS in errors) {
        Text(stringResource(R.string.steps_invalid), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
    }
    TextButton(onClick = { onChange(form.copy(stepsCm = form.stepsCm + "")) }) {
        Icon(Icons.Default.Add, contentDescription = null)
        Text(stringResource(R.string.add_step), modifier = Modifier.padding(start = 8.dp))
    }
}

/** Picks a common base vehicle and wheelbase, which fills in wheelbase and track. */
@Composable
private fun BasePicker(form: VehicleForm, onChange: (VehicleForm) -> Unit) {
    val preset = vehiclePresets.find { it.id == form.presetId }
    PresetDropdown(
        title = R.string.base_vehicle,
        selected = preset?.name,
        placeholder = R.string.base_vehicle_choose,
        options = vehiclePresets.map { it.name },
        onPick = { i ->
            val p = vehiclePresets[i]
            // Keep the wheelbase if this base offers it, else take its longest.
            val keep = p.variants.find { parseDecimal(form.wheelbaseCm) == it.wheelbaseMm / 10 }
            onChange(form.withPreset(p, keep ?: p.variants.last()))
        },
        onCustom = { onChange(form.copy(presetId = null)) },
    )
    if (preset != null) {
        val selected = form.matchingVariant(preset)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            preset.variants.forEach { v ->
                FilterChip(
                    selected = v == selected,
                    onClick = { onChange(form.withPreset(preset, v)) },
                    label = { Text(stringResource(R.string.base_vehicle_wheelbase, displayDecimal(v.wheelbaseMm / 10))) },
                )
            }
        }
        Text(stringResource(R.string.base_vehicle_help), style = MaterialTheme.typography.bodySmall)
        if (preset.approximate) {
            Text(stringResource(R.string.base_vehicle_approximate), style = MaterialTheme.typography.bodySmall)
        }
    }
}

/** Typical caravan sizes as chips; their dimensions are estimates, so the help says how to measure. */
@Composable
private fun CaravanSizePicker(form: VehicleForm, onChange: (VehicleForm) -> Unit) {
    val tandem = form.type == VehicleType.CARAVAN_TANDEM
    val selected = form.matchingSize()
    Text(stringResource(R.string.caravan_size), style = MaterialTheme.typography.labelLarge)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        caravanSizes.filter { it.tandem == tandem }.forEach { size ->
            FilterChip(
                selected = size == selected,
                onClick = { onChange(form.withCaravanSize(size)) },
                label = { Text(stringResource(size.label(), displayDecimal(size.overallLengthMm / 1000))) },
            )
        }
    }
    Text(stringResource(R.string.caravan_size_help), style = MaterialTheme.typography.bodySmall)
}

private fun CaravanSize.label(): Int = when (id) {
    "compact" -> R.string.caravan_size_compact
    "medium" -> R.string.caravan_size_medium
    "large" -> R.string.caravan_size_large
    else -> R.string.caravan_size_tandem
}

/** "Pick a known model" dropdown with a last entry for entering values yourself. */
@Composable
private fun PresetDropdown(
    title: Int,
    selected: String?,
    placeholder: Int,
    options: List<String>,
    onPick: (Int) -> Unit,
    onCustom: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    Text(stringResource(title), style = MaterialTheme.typography.labelLarge)
    Box {
        OutlinedCard(onClick = { menu = true }, modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(selected ?: stringResource(placeholder), modifier = Modifier.weight(1f))
                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
            }
        }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            options.forEachIndexed { i, name ->
                DropdownMenuItem(text = { Text(name) }, onClick = {
                    menu = false
                    onPick(i)
                })
            }
            DropdownMenuItem(text = { Text(stringResource(R.string.preset_custom)) }, onClick = {
                menu = false
                onCustom()
            })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditorScaffold(
    title: Int,
    onBack: () -> Unit,
    onSave: () -> Unit,
    onDelete: (() -> Unit)?,
    deleteTitle: Int,
    deleteText: Int,
    content: @Composable ColumnScope.() -> Unit,
) {
    var confirmDelete by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(title)) },
                navigationIcon = { BackButton(onBack) },
                actions = {
                    if (onDelete != null) {
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete))
                        }
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
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            content()
            Button(onClick = onSave, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.save)) }
        }
    }
    if (confirmDelete && onDelete != null) {
        ConfirmDeleteDialog(deleteTitle, deleteText, onConfirm = { confirmDelete = false; onDelete() }, onDismiss = { confirmDelete = false })
    }
}

@Composable
private fun TextInput(
    label: Int,
    value: String,
    isError: Boolean,
    errorText: Int,
    keyboardType: KeyboardType = KeyboardType.Text,
    help: Int? = null,
    onChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(stringResource(label)) },
        isError = isError,
        supportingText = when {
            isError -> ({ Text(stringResource(errorText)) })
            help != null -> ({ Text(stringResource(help)) })
            else -> null
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun NumberInput(label: Int, value: String, isError: Boolean, errorText: Int, help: Int? = null, onChange: (String) -> Unit) =
    TextInput(label, value, isError, errorText, KeyboardType.Decimal, help, onChange)

fun VehicleType.label(): Int = when (this) {
    VehicleType.MOTORHOME_2AXLE -> R.string.type_motorhome
    VehicleType.CARAVAN_SINGLE -> R.string.type_caravan_single
    VehicleType.CARAVAN_TANDEM -> R.string.type_caravan_tandem
}
