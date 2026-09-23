package io.github.cnissler.levelpitch.profiles

import io.github.cnissler.levelpitch.leveling.DEFAULT_TOLERANCE_DEG
import io.github.cnissler.levelpitch.leveling.Equipment
import io.github.cnissler.levelpitch.leveling.Motorhome
import io.github.cnissler.levelpitch.leveling.PhoneOrientation
import io.github.cnissler.levelpitch.leveling.SingleAxleCaravan
import io.github.cnissler.levelpitch.leveling.TandemCaravan
import io.github.cnissler.levelpitch.leveling.Tilt
import io.github.cnissler.levelpitch.leveling.Vehicle
import io.github.cnissler.levelpitch.leveling.WedgeState
import io.github.cnissler.levelpitch.leveling.Wheel
import kotlinx.serialization.Serializable

@Serializable
enum class VehicleType { MOTORHOME_2AXLE, CARAVAN_SINGLE, CARAVAN_TANDEM }

@Serializable
data class ZeroOffset(val pitchDeg: Double, val rollDeg: Double) {
    fun toTilt() = Tilt(pitchDeg, rollDeg)

    companion object {
        fun of(tilt: Tilt) = ZeroOffset(tilt.pitchDeg, tilt.rollDeg)
    }
}

/** A vehicle as the user set it up. Only the dimensions its [type] needs are set. */
@Serializable
data class VehicleProfile(
    val id: String,
    val name: String,
    val type: VehicleType,
    val trackMm: Double,
    /** Motorhomes only. */
    val wheelbaseMm: Double? = null,
    /** Caravans only: horizontal distance from the (mid-)axle to the hitch. */
    val hitchToAxleMm: Double? = null,
    /** Tandem caravans only: distance between the two axles. */
    val tandemSpacingMm: Double? = null,
    val phoneOrientation: PhoneOrientation = PhoneOrientation.TOP_TO_FRONT,
    /** One zero per orientation, because the sensor bias turns with the phone. */
    val zeroOffsets: Map<PhoneOrientation, ZeroOffset> = emptyMap(),
    val toleranceDeg: Double = DEFAULT_TOLERANCE_DEG,
) {
    /** Zero for the current [phoneOrientation], or null if not calibrated. */
    val zero: Tilt? get() = zeroOffsets[phoneOrientation]?.toTilt()

    /** Geometry for the engine. Throws IllegalArgumentException if a needed dimension is missing or invalid. */
    fun toVehicle(): Vehicle = when (type) {
        VehicleType.MOTORHOME_2AXLE -> Motorhome(required(wheelbaseMm, "wheelbase"), trackMm)
        VehicleType.CARAVAN_SINGLE -> SingleAxleCaravan(trackMm, required(hitchToAxleMm, "hitch distance"))
        VehicleType.CARAVAN_TANDEM -> TandemCaravan(
            trackMm,
            required(tandemSpacingMm, "axle spacing"),
            required(hitchToAxleMm, "hitch distance"),
        )
    }

    fun withZero(orientation: PhoneOrientation, zero: Tilt?): VehicleProfile = copy(
        zeroOffsets = if (zero == null) zeroOffsets - orientation else zeroOffsets + (orientation to ZeroOffset.of(zero)),
    )
}

private fun required(value: Double?, what: String): Double = requireNotNull(value) { "$what missing" }

@Serializable
enum class EquipmentKind {
    /** Wedges with fixed steps. */
    STEPPED,

    /** Devices that lift to any height up to a maximum: curved levellers, side-lift jacks, air bags. */
    CONTINUOUS,
}

/** A set of levelling devices: stepped wedges, or continuous devices lifting up to [maxLiftMm]. */
@Serializable
data class EquipmentProfile(
    val id: String,
    val name: String,
    val stepHeightsMm: List<Double>,
    /** Number of wedges, or of continuous devices. */
    val wedgesOwned: Int,
    val kind: EquipmentKind = EquipmentKind.STEPPED,
    val maxLiftMm: Double? = null,
) {
    /** Throws IllegalArgumentException for invalid steps, lift or counts. */
    fun toEquipment(): Equipment = when (kind) {
        EquipmentKind.STEPPED -> Equipment(stepHeightsMm, wedgesOwned)
        EquipmentKind.CONTINUOUS -> Equipment.continuous(requireNotNull(maxLiftMm) { "maximum lift missing" }, wedgesOwned)
    }
}

@Serializable
enum class MeasurementSource { IMU, CAMERA }

/** One measurement in a session, taken while [wedgeState] was in place. */
@Serializable
data class MeasurementRecord(
    val timestampMs: Long,
    /** Zero-corrected. */
    val pitchDeg: Double,
    val rollDeg: Double,
    val noiseDeg: Double,
    val driftDeg: Double,
    val sampleCount: Int,
    val wedgeState: Map<Wheel, Int>,
    val source: MeasurementSource = MeasurementSource.IMU,
) {
    val tilt: Tilt get() = Tilt(pitchDeg, rollDeg)
}

/** Levelling at one pitch: which wedges are in place and what was measured. */
@Serializable
data class LevelSession(
    val id: String,
    val vehicleId: String,
    val equipmentId: String,
    val wedgeState: Map<Wheel, Int> = emptyMap(),
    val measurements: List<MeasurementRecord> = emptyList(),
    /**
     * Caravans: number of measurements taken before the caravan was unhitched, or null while it is
     * still hitched. Pitch measured while hitched includes the tow car, so only later ones count.
     */
    val unhitchedAfter: Int? = null,
) {
    val lastMeasurement: MeasurementRecord? get() = measurements.lastOrNull()

    val unhitched: Boolean get() = unhitchedAfter != null

    /** A measurement exists from after unhitching. */
    val measuredSinceUnhitching: Boolean get() = unhitchedAfter?.let { measurements.size > it } ?: false

    /** True if wedges changed since the last measurement, so it no longer describes the vehicle. */
    val wedgesChangedSinceMeasurement: Boolean
        get() = lastMeasurement?.let { normalized(it.wedgeState) != normalized(wedgeState) } ?: false
}

/** Drops "no wedge" entries so {FL: 0} and {} compare equal. */
fun normalized(state: WedgeState): WedgeState = state.filterValues { it != 0 }
