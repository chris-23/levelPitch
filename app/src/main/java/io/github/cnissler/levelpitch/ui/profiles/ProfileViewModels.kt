package io.github.cnissler.levelpitch.ui.profiles

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import io.github.cnissler.levelpitch.LevelPitchApp
import io.github.cnissler.levelpitch.profiles.AppData
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID

class ProfilesViewModel(app: LevelPitchApp) : ViewModel() {
    private val repo = app.repository
    val data: StateFlow<AppData> = repo.data

    fun selectVehicle(id: String) = repo.update { it.selectVehicle(id) }
    fun selectEquipment(id: String) = repo.update { it.selectEquipment(id) }
}

/** Editor for a new vehicle ([id] null) or an existing one. */
class VehicleEditViewModel(app: LevelPitchApp, id: String?) : ViewModel() {
    private val repo = app.repository
    private val existing = id?.let { i -> repo.data.value.vehicles.find { it.id == i } }

    val isNew: Boolean = existing == null
    var form by mutableStateOf(existing?.let(VehicleForm::from) ?: VehicleForm.newDefault())
    /** Errors show only after the first save attempt, not while typing a new profile. */
    var showErrors by mutableStateOf(false)
        private set

    /** Saves and returns true, or marks the errors and returns false. */
    fun save(): Boolean {
        val profile = form.toProfile(existing?.id ?: UUID.randomUUID().toString(), existing)
        if (profile == null) {
            showErrors = true
            return false
        }
        repo.update { it.upsertVehicle(profile) }
        return true
    }

    fun delete() {
        existing?.let { e -> repo.update { it.deleteVehicle(e.id) } }
    }
}

class EquipmentEditViewModel(app: LevelPitchApp, id: String?) : ViewModel() {
    private val repo = app.repository
    private val existing = id?.let { i -> repo.data.value.equipment.find { it.id == i } }

    val isNew: Boolean = existing == null
    var form by mutableStateOf(existing?.let(EquipmentForm::from) ?: EquipmentForm())
    var showErrors by mutableStateOf(false)
        private set

    fun save(): Boolean {
        val profile = form.toProfile(existing?.id ?: UUID.randomUUID().toString())
        if (profile == null) {
            showErrors = true
            return false
        }
        repo.update { it.upsertEquipment(profile) }
        return true
    }

    fun delete() {
        existing?.let { e -> repo.update { it.deleteEquipment(e.id) } }
    }
}
