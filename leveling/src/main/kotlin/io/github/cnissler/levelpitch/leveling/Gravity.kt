package io.github.cnissler.levelpitch.leveling

import kotlin.math.asin
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Which way the phone's top edge points while it lies screen-up in the vehicle.
 * [degrees] is the rotation from the default, counter-clockwise seen from above.
 */
enum class PhoneOrientation(val degrees: Int) {
    TOP_TO_FRONT(0),
    TOP_TO_LEFT(90),
    TOP_TO_REAR(180),
    TOP_TO_RIGHT(270);

    /**
     * Phone frame (Android sensor axes: x to the right edge, y to the top edge, z out of the
     * screen) to vehicle frame (x forward, y left, z up).
     */
    fun toVehicle(v: Vec3): Vec3 = when (this) {
        TOP_TO_FRONT -> Vec3(v.y, -v.x, v.z)
        TOP_TO_LEFT -> Vec3(v.x, v.y, v.z)
        TOP_TO_REAR -> Vec3(-v.y, v.x, v.z)
        TOP_TO_RIGHT -> Vec3(-v.x, -v.y, v.z)
    }
}

/**
 * Tilt from the up direction in the vehicle frame (at rest: the mean accelerometer reading).
 * Each angle is what a spirit level along that vehicle axis shows, asin of the up component
 * along the axis. [Tilt.heightAtMm]'s tan form overstates heights by 1/cos of the angle
 * (0.4 % at 5°), far below one wedge step.
 */
fun tiltFromUp(up: Vec3): Tilt {
    val u = up.normalized()
    return Tilt(Math.toDegrees(asin(u.x)), Math.toDegrees(asin(u.y)))
}

/** Inverse of [tiltFromUp]: the unit up vector in the vehicle frame. */
fun upFromTilt(tilt: Tilt): Vec3 {
    val sx = sin(Math.toRadians(tilt.pitchDeg))
    val sy = sin(Math.toRadians(tilt.rollDeg))
    val zz = 1 - sx * sx - sy * sy
    require(zz > 0) { "tilt too steep to be a spirit-level reading: $tilt" }
    return Vec3(sx, sy, sqrt(zz))
}
