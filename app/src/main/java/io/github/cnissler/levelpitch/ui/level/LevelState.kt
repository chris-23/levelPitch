package io.github.cnissler.levelpitch.ui.level

import io.github.cnissler.levelpitch.leveling.Caravan
import io.github.cnissler.levelpitch.leveling.Equipment
import io.github.cnissler.levelpitch.leveling.Measurement
import io.github.cnissler.levelpitch.leveling.Motorhome
import io.github.cnissler.levelpitch.leveling.PhoneOrientation
import io.github.cnissler.levelpitch.leveling.Recommendation
import io.github.cnissler.levelpitch.leveling.SingleAxleCaravan
import io.github.cnissler.levelpitch.leveling.TandemCaravan
import io.github.cnissler.levelpitch.leveling.Tilt
import io.github.cnissler.levelpitch.leveling.Vehicle
import io.github.cnissler.levelpitch.leveling.WedgeState
import io.github.cnissler.levelpitch.leveling.Wheel
import io.github.cnissler.levelpitch.leveling.WindowResult
import io.github.cnissler.levelpitch.leveling.isLevel
import io.github.cnissler.levelpitch.leveling.recommend
import io.github.cnissler.levelpitch.profiles.AppData
import io.github.cnissler.levelpitch.profiles.EquipmentProfile
import io.github.cnissler.levelpitch.profiles.LevelSession
import io.github.cnissler.levelpitch.profiles.VehicleProfile
import io.github.cnissler.levelpitch.profiles.normalized
import kotlin.math.abs
import kotlin.math.atan

/** What the level screen can work with. */
sealed interface Setup {
    data object NoVehicle : Setup
    data object NoEquipment : Setup

    /** Stored profile values the engine rejects (only possible with a hand-edited file). */
    data class Invalid(val message: String?) : Setup

    data class Ready(
        val vehicleProfile: VehicleProfile,
        val equipmentProfile: EquipmentProfile,
        val vehicle: Vehicle,
        val equipment: Equipment,
    ) : Setup
}

/** Move the wedges under [wheels] (one raise group) from [fromStep] to [toStep]. */
data class WedgeChange(val wheels: List<Wheel>, val fromStep: Int, val toStep: Int)

/** Outcome of the last measurement of a session. */
data class LevelPlan(
    /** Zero-corrected tilt as measured. */
    val tilt: Tilt,
    val isLevel: Boolean,
    val recommendation: Recommendation,
    /** Changes relative to the wedges the measurement was taken on; empty once level. */
    val changes: List<WedgeChange>,
    /** Wedges changed since the measurement, so it no longer describes the vehicle. */
    val stale: Boolean,
    /** The wedges now in place are the recommended ones. */
    val applied: Boolean,
)

/** The most the equipment can correct: the highest step (or lift) across the wheelbase or the track. */
data class Capacity(
    val maxLiftMm: Double,
    /** Motorhomes only; caravans level front to back with the jockey wheel. */
    val frontToBackDeg: Double?,
    val sideToSideDeg: Double,
)

fun capacity(vehicle: Vehicle, equipment: Equipment): Capacity {
    val lift = equipment.stepHeightsMm.last()
    fun deg(spanMm: Double) = Math.toDegrees(atan(lift / spanMm))
    return when (vehicle) {
        is Motorhome -> Capacity(lift, deg(vehicle.wheelbaseMm), deg(vehicle.trackMm))
        is SingleAxleCaravan -> Capacity(lift, null, deg(vehicle.trackMm))
        is TandemCaravan -> Capacity(lift, null, deg(vehicle.trackMm))
    }
}

/**
 * Where a caravan is in the usual procedure: level side to side with wedges while hitched, chock
 * and unhitch, level front to back with the jockey wheel, then lower the corner steadies (which
 * only stabilise; they must never lift the caravan).
 */
enum class CaravanStep { SIDE_TO_SIDE, UNHITCH, FRONT_TO_BACK, STEADIES }

fun caravanStep(session: LevelSession?, plan: LevelPlan?, toleranceDeg: Double): CaravanStep {
    val current = plan?.takeIf { !it.stale }
    if (session == null || !session.unhitched) {
        // Hitched: only roll counts; pitch includes the tow car.
        val rollLevel = current != null && abs(current.tilt.rollDeg) <= toleranceDeg
        return if (rollLevel) CaravanStep.UNHITCH else CaravanStep.SIDE_TO_SIDE
    }
    val levelSinceUnhitching = session.measuredSinceUnhitching && current?.isLevel == true
    return if (levelSinceUnhitching) CaravanStep.STEADIES else CaravanStep.FRONT_TO_BACK
}

data class LevelUiState(
    val setup: Setup,
    val sensorAvailable: Boolean,
    val orientation: PhoneOrientation = PhoneOrientation.TOP_TO_FRONT,
    /** A zero exists for [orientation]. */
    val calibrated: Boolean = false,
    val wedgeState: WedgeState = emptyMap(),
    val plan: LevelPlan? = null,
    val running: Boolean = false,
    /** The last measurement attempt, if it was rejected. */
    val rejected: WindowResult? = null,
    /** Caravans only. */
    val caravanStep: CaravanStep? = null,
    /** Caravans: a measurement exists from after unhitching, so the jockey advice applies. */
    val measuredSinceUnhitching: Boolean = false,
)

fun levelUiState(data: AppData, sensorAvailable: Boolean, running: Boolean, rejected: WindowResult?): LevelUiState {
    val vp = data.activeVehicle ?: return LevelUiState(Setup.NoVehicle, sensorAvailable)
    val ep = data.activeEquipment ?: return LevelUiState(Setup.NoEquipment, sensorAvailable)
    val setup = try {
        Setup.Ready(vp, ep, vp.toVehicle(), ep.toEquipment())
    } catch (e: IllegalArgumentException) {
        return LevelUiState(Setup.Invalid(e.message), sensorAvailable)
    }
    val session = data.activeSession
    val plan = session?.let { planFor(setup, it) }
    return LevelUiState(
        setup = setup,
        sensorAvailable = sensorAvailable,
        orientation = vp.phoneOrientation,
        calibrated = vp.zero != null,
        wedgeState = session?.wedgeState.orEmpty(),
        plan = plan,
        running = running,
        rejected = rejected,
        caravanStep = if (setup.vehicle is Caravan) caravanStep(session, plan, vp.toleranceDeg) else null,
        measuredSinceUnhitching = session?.measuredSinceUnhitching == true,
    )
}

/** Plan from the session's last measurement, or null if there is none (or it no longer fits the profiles). */
fun planFor(setup: Setup.Ready, session: LevelSession): LevelPlan? {
    val m = session.lastMeasurement ?: return null
    val vehicle = setup.vehicle
    val atMeasurement = m.wedgeState
    val rec = try {
        recommend(vehicle, setup.equipment, Measurement.VehicleTilt(m.tilt), current = atMeasurement)
    } catch (e: IllegalArgumentException) {
        return null
    }
    val level = isLevel(vehicle, m.tilt, setup.vehicleProfile.toleranceDeg)
    val changes = if (level) {
        emptyList()
    } else {
        vehicle.raiseGroups.mapNotNull { group ->
            val from = atMeasurement[group.first()] ?: 0
            val to = rec.steps.getValue(group.first())
            if (from != to) WedgeChange(group, from, to) else null
        }
    }
    return LevelPlan(
        tilt = m.tilt,
        isLevel = level,
        recommendation = rec,
        changes = changes,
        stale = session.wedgesChangedSinceMeasurement,
        applied = normalized(session.wedgeState) == normalized(rec.steps),
    )
}
