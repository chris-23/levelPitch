package io.github.cnissler.levelpitch.leveling

import kotlin.math.abs

/**
 * Whether a measured [tilt] counts as level for [vehicle]: total tilt within [toleranceDeg] for
 * motorhomes; for caravans roll (wedges) and pitch (jockey wheel) each within [toleranceDeg].
 */
fun isLevel(vehicle: Vehicle, tilt: Tilt, toleranceDeg: Double = DEFAULT_TOLERANCE_DEG): Boolean = when (vehicle) {
    is Motorhome -> tilt.totalDeg <= toleranceDeg
    is Caravan -> abs(tilt.rollDeg) <= toleranceDeg && abs(tilt.pitchDeg) <= toleranceDeg
}
