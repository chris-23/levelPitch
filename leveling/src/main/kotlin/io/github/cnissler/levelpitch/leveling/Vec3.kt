package io.github.cnissler.levelpitch.leveling

import kotlin.math.sqrt

/** Three-component vector; accelerometer readings are in m/s². */
data class Vec3(val x: Double, val y: Double, val z: Double) {
    val norm: Double get() = sqrt(x * x + y * y + z * z)

    operator fun plus(o: Vec3) = Vec3(x + o.x, y + o.y, z + o.z)
    operator fun times(s: Double) = Vec3(x * s, y * s, z * s)
    operator fun div(s: Double) = Vec3(x / s, y / s, z / s)
    infix fun dot(o: Vec3): Double = x * o.x + y * o.y + z * o.z
    infix fun cross(o: Vec3) = Vec3(y * o.z - z * o.y, z * o.x - x * o.z, x * o.y - y * o.x)
    fun normalized(): Vec3 = this / norm

    companion object {
        val UP = Vec3(0.0, 0.0, 1.0)

        fun mean(vs: List<Vec3>): Vec3 = vs.reduce(Vec3::plus) / vs.size.toDouble()
    }
}
