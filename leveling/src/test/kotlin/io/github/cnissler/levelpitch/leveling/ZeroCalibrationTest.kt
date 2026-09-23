package io.github.cnissler.levelpitch.leveling

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.cos
import kotlin.math.sin

class ZeroCalibrationTest {

    @Test
    fun noOffsetChangesNothing() {
        val t = Tilt(1.2, -0.7).relativeTo(Tilt.LEVEL)
        assertEquals(1.2, t.pitchDeg, 1e-12)
        assertEquals(-0.7, t.rollDeg, 1e-12)
    }

    @Test
    fun readingTheZeroGivesLevel() {
        val zero = Tilt(0.8, -1.5)
        val t = zero.relativeTo(zero)
        assertEquals(0.0, t.pitchDeg, 1e-12)
        assertEquals(0.0, t.rollDeg, 1e-12)
    }

    @Test
    fun offsetAlongOneAxisSubtracts() {
        val t = Tilt(3.0, 0.0).relativeTo(Tilt(1.0, 0.0))
        assertEquals(2.0, t.pitchDeg, 1e-12)
        assertEquals(0.0, t.rollDeg, 1e-12)
    }

    @Test
    fun smallOffsetsRoughlySubtract() {
        val t = Tilt(1.5, 0.7).relativeTo(Tilt(0.5, -0.3))
        assertEquals(1.0, t.pitchDeg, 0.01)
        assertEquals(1.0, t.rollDeg, 0.01)
    }

    @Test
    fun recoversVehicleTiltThroughTiltedSurfaceExactly() {
        // Measuring surface tilted 3° about a horizontal axis relative to the vehicle.
        val surface = { v: Vec3 -> rotate(v, Vec3(0.6, 0.8, 0.0), 3.0) }
        val zero = tiltFromUp(surface(Vec3.UP))
        val vehicle = Tilt(pitchDeg = 2.5, rollDeg = -1.2)
        val reading = tiltFromUp(surface(upFromTilt(vehicle)))

        val t = reading.relativeTo(zero)
        assertEquals(2.5, t.pitchDeg, 1e-9)
        assertEquals(-1.2, t.rollDeg, 1e-9)
    }

    /** Rotates [v] by [deg] about the unit [axis]. */
    private fun rotate(v: Vec3, axis: Vec3, deg: Double): Vec3 {
        val a = Math.toRadians(deg)
        return v * cos(a) + (axis cross v) * sin(a) + axis * ((axis dot v) * (1 - cos(a)))
    }
}
