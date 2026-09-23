package io.github.cnissler.levelpitch.leveling

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.atan
import kotlin.math.tan

class TiltTest {

    @Test
    fun levelIsZeroEverywhere() {
        assertEquals(0.0, Tilt.LEVEL.heightAtMm(1750.0, 800.0), 0.0)
        assertEquals(0.0, Tilt.LEVEL.heightAtMm(-1750.0, -800.0), 0.0)
    }

    @Test
    fun positivePitchMeansNoseUp() {
        val tilt = Tilt(pitchDeg = 1.0, rollDeg = 0.0)
        assertTrue(tilt.heightAtMm(1000.0, 0.0) > 0.0)
        assertTrue(tilt.heightAtMm(-1000.0, 0.0) < 0.0)
        assertEquals(0.0, tilt.heightAtMm(0.0, 800.0), 1e-12)
    }

    @Test
    fun positiveRollMeansLeftSideUp() {
        val tilt = Tilt(pitchDeg = 0.0, rollDeg = 1.0)
        assertTrue(tilt.heightAtMm(0.0, 800.0) > 0.0)
        assertTrue(tilt.heightAtMm(0.0, -800.0) < 0.0)
        assertEquals(0.0, tilt.heightAtMm(1000.0, 0.0), 1e-12)
    }

    @Test
    fun oneCentimetreOver573MillimetresIsAboutOneDegree() {
        // Bench protocol from SPEC.md: 1 cm shim over 57.3 cm ≈ 1°.
        assertEquals(10.0, Tilt(1.0, 0.0).heightAtMm(573.0, 0.0), 0.01)
    }

    @Test
    fun pitchAndRollAdd() {
        val tilt = Tilt(pitchDeg = 2.0, rollDeg = -1.0)
        val expected = 1750.0 * tan(Math.toRadians(2.0)) + 800.0 * tan(Math.toRadians(-1.0))
        assertEquals(expected, tilt.heightAtMm(1750.0, 800.0), 1e-9)
    }

    @Test
    fun fromSlopesInvertsHeightAt() {
        val tilt = Tilt.fromSlopes(tan(Math.toRadians(2.0)), tan(Math.toRadians(-1.0)))
        assertEquals(2.0, tilt.pitchDeg, 1e-12)
        assertEquals(-1.0, tilt.rollDeg, 1e-12)
    }

    @Test
    fun totalCombinesSlopes() {
        assertEquals(1.5, Tilt(1.5, 0.0).totalDeg, 1e-12)
        assertEquals(1.5, Tilt(0.0, -1.5).totalDeg, 1e-12)
        // Slopes 0.03 and 0.04 give a steepest slope of 0.05.
        assertEquals(Math.toDegrees(atan(0.05)), Tilt.fromSlopes(0.03, 0.04).totalDeg, 1e-12)
        assertEquals(0.0, Tilt.LEVEL.totalDeg, 0.0)
    }
}
