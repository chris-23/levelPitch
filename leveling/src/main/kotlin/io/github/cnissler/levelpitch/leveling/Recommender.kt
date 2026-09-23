package io.github.cnissler.levelpitch.leveling

import kotlin.math.abs
import kotlin.math.atan

const val DEFAULT_TOLERANCE_DEG = 0.5

/**
 * Candidates whose residuals differ by less than this count as equally good, so the
 * tie-breaks decide. Well below the ≤ 0.2° accuracy target.
 */
const val RESIDUAL_TIE_DEG = 0.01

/** The user's stepped wedges: one wheel stands on one step of one wedge. */
data class Equipment(val stepHeightsMm: List<Double>, val wedgesOwned: Int) {
    init {
        require(stepHeightsMm.isNotEmpty()) { "need at least one step" }
        require(stepHeightsMm.first() > 0 && stepHeightsMm.zipWithNext().all { (a, b) -> b > a }) {
            "step heights must be positive and increasing"
        }
        require(wedgesOwned >= 0) { "wedgesOwned must not be negative" }
    }

    /** Valid step numbers, 0 meaning no wedge. */
    val steps: IntRange get() = 0..stepHeightsMm.size

    fun heightMm(step: Int): Double = if (step == 0) 0.0 else stepHeightsMm[step - 1]
}

/** Wedge step per wheel, 1-based into [Equipment.stepHeightsMm]; 0 or absent = no wedge. */
typealias WedgeState = Map<Wheel, Int>

sealed interface Measurement {
    /** Simple mode: vehicle tilt from the phone. */
    data class VehicleTilt(val tilt: Tilt) : Measurement

    /** Camera mode: ground heights at every wheel contact point, in mm. */
    data class GroundHeights(val heightsMm: Map<Wheel, Double>) : Measurement
}

data class Recommendation(
    /** Target step for every wheel of the vehicle (0 = no wedge). */
    val steps: WedgeState,
    /** Pitch left after following all advice; null if unknown (caravan without a tilt measurement). */
    val residualPitchDeg: Double?,
    /** Roll left after following all advice. */
    val residualRollDeg: Double,
    /** What the search minimises: total tilt for motorhomes, |roll| for caravans. */
    val residualDeg: Double,
    /**
     * Caravans: move the hitch up (+) or down (−) by this many mm with the jockey wheel,
     * after the wedges are in place. Null for motorhomes and when pitch is unknown.
     */
    val hitchAdjustMm: Double?,
) {
    val wedgeCount: Int get() = steps.values.count { it > 0 }

    fun isWithin(toleranceDeg: Double = DEFAULT_TOLERANCE_DEG): Boolean = residualDeg <= toleranceDeg
}

/**
 * Best wedge steps for [vehicle] given [measurement]: brute force over all wedge states within
 * [Equipment.wedgesOwned], minimising the residual tilt of the least-squares plane through the
 * raised contact points. Ties (within [RESIDUAL_TIE_DEG]) go to fewer wedges, then lower wedges.
 */
fun recommend(vehicle: Vehicle, equipment: Equipment, measurement: Measurement): Recommendation {
    val ground = when (measurement) {
        is Measurement.VehicleTilt -> vehicle.contactHeightsMm(measurement.tilt)
        is Measurement.GroundHeights -> {
            require(measurement.heightsMm.keys == vehicle.wheels.keys) { "need a height for every wheel" }
            measurement.heightsMm
        }
    }
    // Only a tilt measurement tells where the hitch is; ground heights say nothing about the body.
    val hitchMm = if (vehicle is Caravan && measurement is Measurement.VehicleTilt) {
        measurement.tilt.heightAtMm(vehicle.hitch.xMm, vehicle.hitch.yMm)
    } else {
        null
    }

    val candidates = wedgeStates(vehicle.raiseGroups, equipment)
        .map { evaluate(vehicle, equipment, ground, hitchMm, it) }
    val best = candidates.minOf { it.residualDeg }
    return candidates
        .filter { it.residualDeg < best + RESIDUAL_TIE_DEG }
        .minWith(
            compareBy<Recommendation> { it.wedgeCount }
                .thenBy { r -> r.steps.values.sumOf { equipment.heightMm(it) } },
        )
}

/** Every assignment of a step to each raise group that needs at most [Equipment.wedgesOwned] wedges. */
private fun wedgeStates(groups: List<List<Wheel>>, equipment: Equipment): List<WedgeState> {
    var states = listOf(emptyMap<Wheel, Int>())
    for (group in groups) {
        states = states.flatMap { state -> equipment.steps.map { step -> state + group.associateWith { step } } }
    }
    return states.filter { state -> state.values.count { it > 0 } <= equipment.wedgesOwned }
}

private fun evaluate(
    vehicle: Vehicle,
    equipment: Equipment,
    groundMm: Map<Wheel, Double>,
    hitchMm: Double?,
    steps: WedgeState,
): Recommendation {
    val raised = groundMm.mapValues { (wheel, z) -> z + equipment.heightMm(steps.getValue(wheel)) }
    val slopes = fitSlopes(vehicle.wheels.map { (wheel, p) -> p to raised.getValue(wheel) })
    // Every layout spreads across the track, so roll is always determined.
    val rollDeg = Math.toDegrees(atan(slopes.y!!))
    return when (vehicle) {
        is Motorhome -> {
            val tilt = Tilt.fromSlopes(slopes.x!!, slopes.y)
            Recommendation(steps, tilt.pitchDeg, tilt.rollDeg, tilt.totalDeg, hitchAdjustMm = null)
        }
        is Caravan -> {
            // The wedges lift the axle while the hitch stays on the jockey wheel. Pitch is level once
            // the hitch is at axle height; the mean wheel height is the height at the axle centre.
            val hitchAdjust = hitchMm?.let { raised.values.average() - it }
            Recommendation(steps, hitchAdjust?.let { 0.0 }, rollDeg, abs(rollDeg), hitchAdjust)
        }
    }
}
