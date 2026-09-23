package io.github.cnissler.levelpitch.leveling

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.cos
import kotlin.math.sin

class GravityTest {

    private val g = 9.81

    /** Phone reading when its top edge is raised by [deg] (the phone tipped toward its bottom edge). */
    private fun topEdgeUp(deg: Double) =
        Vec3(0.0, g * sin(Math.toRadians(deg)), g * cos(Math.toRadians(deg)))

    /** Phone reading when its right edge is raised by [deg]. */
    private fun rightEdgeUp(deg: Double) =
        Vec3(g * sin(Math.toRadians(deg)), 0.0, g * cos(Math.toRadians(deg)))

    private fun tiltOf(orientation: PhoneOrientation, reading: Vec3) = tiltFromUp(orientation.toVehicle(reading))

    @Test
    fun flatPhoneOnLevelVehicleReadsLevel() {
        for (o in PhoneOrientation.entries) {
            val t = tiltOf(o, Vec3(0.0, 0.0, g))
            assertEquals(0.0, t.pitchDeg, 0.0)
            assertEquals(0.0, t.rollDeg, 0.0)
        }
    }

    @Test
    fun topTowardFront() {
        // Top edge = front: raised top edge means nose up; raised right edge means right side up.
        assertEquals(Tilt(2.0, 0.0), tiltOf(PhoneOrientation.TOP_TO_FRONT, topEdgeUp(2.0)).rounded())
        assertEquals(Tilt(0.0, -2.0), tiltOf(PhoneOrientation.TOP_TO_FRONT, rightEdgeUp(2.0)).rounded())
    }

    @Test
    fun topTowardLeft() {
        // Top edge = left side, right edge = front.
        assertEquals(Tilt(0.0, 2.0), tiltOf(PhoneOrientation.TOP_TO_LEFT, topEdgeUp(2.0)).rounded())
        assertEquals(Tilt(2.0, 0.0), tiltOf(PhoneOrientation.TOP_TO_LEFT, rightEdgeUp(2.0)).rounded())
    }

    @Test
    fun topTowardRear() {
        // Top edge = rear, right edge = left side.
        assertEquals(Tilt(-2.0, 0.0), tiltOf(PhoneOrientation.TOP_TO_REAR, topEdgeUp(2.0)).rounded())
        assertEquals(Tilt(0.0, 2.0), tiltOf(PhoneOrientation.TOP_TO_REAR, rightEdgeUp(2.0)).rounded())
    }

    @Test
    fun topTowardRight() {
        // Top edge = right side, right edge = rear.
        assertEquals(Tilt(0.0, -2.0), tiltOf(PhoneOrientation.TOP_TO_RIGHT, topEdgeUp(2.0)).rounded())
        assertEquals(Tilt(-2.0, 0.0), tiltOf(PhoneOrientation.TOP_TO_RIGHT, rightEdgeUp(2.0)).rounded())
    }

    @Test
    fun orientationDegreesAreCounterClockwise() {
        assertEquals(listOf(0, 90, 180, 270), PhoneOrientation.entries.map { it.degrees })
    }

    @Test
    fun singleAxisTiltIsExactAngle() {
        val t = tiltFromUp(Vec3(sin(Math.toRadians(3.7)), 0.0, cos(Math.toRadians(3.7))))
        assertEquals(3.7, t.pitchDeg, 1e-12)
    }

    @Test
    fun ignoresVectorLength() {
        val a = tiltFromUp(Vec3(0.1, -0.2, 1.0))
        val b = tiltFromUp(Vec3(0.1, -0.2, 1.0) * 9.81)
        assertEquals(a.pitchDeg, b.pitchDeg, 1e-12)
        assertEquals(a.rollDeg, b.rollDeg, 1e-12)
    }

    @Test
    fun upFromTiltInvertsTiltFromUp() {
        val tilt = Tilt(pitchDeg = 2.5, rollDeg = -1.3)
        val up = upFromTilt(tilt)
        assertEquals(1.0, up.norm, 1e-12)
        val back = tiltFromUp(up)
        assertEquals(2.5, back.pitchDeg, 1e-12)
        assertEquals(-1.3, back.rollDeg, 1e-12)
        assertTrue(up.z > 0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun upFromTiltRejectsImpossibleTilt() {
        upFromTilt(Tilt(60.0, 60.0))
    }

    private fun Tilt.rounded() = Tilt(Math.round(pitchDeg * 1e9) / 1e9, Math.round(rollDeg * 1e9) / 1e9)
}
