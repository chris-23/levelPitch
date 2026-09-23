package io.github.cnissler.levelpitch.leveling

/**
 * This raw reading corrected by a zero offset. [zero] is the raw tilt read while the vehicle
 * was level, so it holds both the measuring surface's misalignment and the sensor bias.
 *
 * The reading is rotated by the smallest rotation that takes the zero's up direction to
 * vertical. That is exact when the surface is tilted, but not turned (yawed), relative to the
 * vehicle; a turned phone mixes pitch into roll and is not correctable from the zero alone.
 */
fun Tilt.relativeTo(zero: Tilt): Tilt {
    val u0 = upFromTilt(zero)
    val axis = u0 cross Vec3.UP
    val sin = axis.norm
    if (sin < 1e-12) return this
    val k = axis / sin
    val cos = u0 dot Vec3.UP
    val u = upFromTilt(this)
    // Rodrigues' rotation formula.
    val rotated = u * cos + (k cross u) * sin + k * ((k dot u) * (1 - cos))
    return tiltFromUp(rotated)
}
