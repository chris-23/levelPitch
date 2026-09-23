package io.github.cnissler.levelpitch.profiles

import io.github.cnissler.levelpitch.leveling.PhoneOrientation
import io.github.cnissler.levelpitch.leveling.Tilt
import io.github.cnissler.levelpitch.leveling.WedgeState
import kotlinx.serialization.Serializable
import java.util.UUID

/** Everything the app stores. All changes go through the pure functions below. */
@Serializable
data class AppData(
    val version: Int = 1,
    val vehicles: List<VehicleProfile> = emptyList(),
    val equipment: List<EquipmentProfile> = emptyList(),
    val activeVehicleId: String? = null,
    val activeEquipmentId: String? = null,
    val session: LevelSession? = null,
    /** The first-start tutorial was finished or skipped. */
    val tutorialSeen: Boolean = false,
) {
    fun markTutorialSeen(): AppData = copy(tutorialSeen = true)

    val activeVehicle: VehicleProfile? get() = vehicles.find { it.id == activeVehicleId }
    val activeEquipment: EquipmentProfile? get() = equipment.find { it.id == activeEquipmentId }

    /** The session for the active vehicle and wedge set; null if none is running or they changed. */
    val activeSession: LevelSession?
        get() = session?.takeIf { it.vehicleId == activeVehicleId && it.equipmentId == activeEquipmentId }

    /**
     * Adds or replaces a vehicle; the first one becomes active. Changing the type ends its session,
     * because the wheels no longer match the recorded wedge state.
     */
    fun upsertVehicle(v: VehicleProfile): AppData {
        val old = vehicles.find { it.id == v.id }
        return copy(
            vehicles = vehicles.upsert(v) { it.id },
            activeVehicleId = activeVehicleId ?: v.id,
            session = session?.takeUnless { it.vehicleId == v.id && old != null && old.type != v.type },
        )
    }

    /** Adds or replaces a wedge set; changing its steps or lift ends its session (recorded steps would be wrong). */
    fun upsertEquipment(e: EquipmentProfile): AppData {
        val old = equipment.find { it.id == e.id }
        return copy(
            equipment = equipment.upsert(e) { it.id },
            activeEquipmentId = activeEquipmentId ?: e.id,
            session = session?.takeUnless {
                it.equipmentId == e.id && old != null &&
                    (old.stepHeightsMm != e.stepHeightsMm || old.kind != e.kind || old.maxLiftMm != e.maxLiftMm)
            },
        )
    }

    /** Removes a vehicle; if it was active, the first remaining one takes over. */
    fun deleteVehicle(id: String): AppData {
        val rest = vehicles.filterNot { it.id == id }
        return copy(
            vehicles = rest,
            activeVehicleId = if (activeVehicleId == id) rest.firstOrNull()?.id else activeVehicleId,
            session = session?.takeIf { it.vehicleId != id },
        )
    }

    fun deleteEquipment(id: String): AppData {
        val rest = equipment.filterNot { it.id == id }
        return copy(
            equipment = rest,
            activeEquipmentId = if (activeEquipmentId == id) rest.firstOrNull()?.id else activeEquipmentId,
            session = session?.takeIf { it.equipmentId != id },
        )
    }

    fun selectVehicle(id: String): AppData = if (vehicles.any { it.id == id }) copy(activeVehicleId = id) else this

    fun selectEquipment(id: String): AppData = if (equipment.any { it.id == id }) copy(activeEquipmentId = id) else this

    /** Stores (or with null clears) the zero of the active vehicle for [orientation]. */
    fun setZero(orientation: PhoneOrientation, zero: Tilt?): AppData =
        updateActiveVehicle { it.withZero(orientation, zero) }

    fun setOrientation(orientation: PhoneOrientation): AppData =
        updateActiveVehicle { it.copy(phoneOrientation = orientation) }

    /** Starts a fresh session (no wedges placed) for the active vehicle and wedge set. */
    fun newSession(newId: String = UUID.randomUUID().toString()): AppData {
        val v = activeVehicleId ?: return this
        val e = activeEquipmentId ?: return this
        return copy(session = LevelSession(newId, v, e))
    }

    fun recordMeasurement(m: MeasurementRecord): AppData =
        updateSession { it.copy(measurements = it.measurements + m) }

    fun setWedgeState(state: WedgeState): AppData = updateSession { it.copy(wedgeState = normalized(state)) }

    /** Caravans: marks the caravan unhitched (or hitched again with false). */
    fun setUnhitched(unhitched: Boolean): AppData =
        updateSession { it.copy(unhitchedAfter = if (unhitched) it.measurements.size else null) }

    private fun updateActiveVehicle(change: (VehicleProfile) -> VehicleProfile): AppData {
        val v = activeVehicle ?: return this
        return copy(vehicles = vehicles.upsert(change(v)) { it.id })
    }

    /** Applies [change] to the active session, starting one if needed. */
    private fun updateSession(change: (LevelSession) -> LevelSession): AppData {
        val base = if (activeSession == null) newSession() else this
        val s = base.activeSession ?: return this
        return base.copy(session = change(s))
    }
}

private fun <T> List<T>.upsert(item: T, key: (T) -> String): List<T> {
    val i = indexOfFirst { key(it) == key(item) }
    return if (i < 0) this + item else toMutableList().also { it[i] = item }
}
