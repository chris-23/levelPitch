package io.github.cnissler.levelpitch.leveling

import kotlin.math.atan
import kotlin.math.hypot
import kotlin.math.tan

/**
 * Vehicle tilt in degrees: pitch positive = nose up, roll positive = left side up.
 *
 * Defined by the slopes of the plane under the wheels in the vehicle frame
 * (x forward, y left, z up): z = x·tan(pitch) + y·tan(roll).
 */
data class Tilt(val pitchDeg: Double, val rollDeg: Double) {

    /** Height in mm of the point ([xMm], [yMm]) relative to the frame origin. */
    fun heightAtMm(xMm: Double, yMm: Double): Double =
        xMm * tan(Math.toRadians(pitchDeg)) + yMm * tan(Math.toRadians(rollDeg))

    /** Angle between the plane and the horizontal, combining pitch and roll. Never negative. */
    val totalDeg: Double
        get() = Math.toDegrees(atan(hypot(tan(Math.toRadians(pitchDeg)), tan(Math.toRadians(rollDeg)))))

    companion object {
        val LEVEL = Tilt(0.0, 0.0)

        /** Tilt of the plane z = [slopeX]·x + [slopeY]·y (dimensionless slopes, e.g. mm per mm). */
        fun fromSlopes(slopeX: Double, slopeY: Double): Tilt =
            Tilt(Math.toDegrees(atan(slopeX)), Math.toDegrees(atan(slopeY)))
    }
}
