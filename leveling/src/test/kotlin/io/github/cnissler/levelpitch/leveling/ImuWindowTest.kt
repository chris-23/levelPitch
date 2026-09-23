package io.github.cnissler.levelpitch.leveling

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.cos
import kotlin.math.sin

class ImuWindowTest {

    // TOP_TO_LEFT maps phone axes 1:1 onto vehicle axes, so readings can be built from vehicle tilts.
    private val identity = PhoneOrientation.TOP_TO_LEFT

    private fun reading(pitchDeg: Double, rollDeg: Double) = upFromTilt(Tilt(pitchDeg, rollDeg)) * 9.81

    private fun still(result: WindowResult): ImuReading {
        assertTrue("expected Still, got $result", result is WindowResult.Still)
        return (result as WindowResult.Still).reading
    }

    @Test
    fun constantSamplesAverageToTheirTilt() {
        val r = still(analyzeWindow(List(200) { reading(1.5, -0.5) }, identity))
        assertEquals(1.5, r.tilt.pitchDeg, 1e-9)
        assertEquals(-0.5, r.tilt.rollDeg, 1e-9)
        assertEquals(0.0, r.noiseDeg, 1e-9)
        assertEquals(0.0, r.driftDeg, 1e-9)
        assertEquals(200, r.sampleCount)
    }

    @Test
    fun sensorNoiseAveragesOut() {
        // ±0.1° jitter, typical single-sample noise, alternating so every quarter averages to the mean.
        val samples = List(200) { i -> if (i % 2 == 0) reading(1.1, 0.0) else reading(0.9, 0.0) }
        val r = still(analyzeWindow(samples, identity))
        assertEquals(1.0, r.tilt.pitchDeg, 1e-4)
        assertEquals(0.1, r.noiseDeg, 1e-3)
    }

    @Test
    fun vibrationIsRejected() {
        val samples = List(200) { i -> if (i % 2 == 0) reading(2.0, 0.0) else reading(0.0, 0.0) }
        val result = analyzeWindow(samples, identity)
        assertTrue(result is WindowResult.Moved)
        assertEquals(1.0, (result as WindowResult.Moved).reading.noiseDeg, 1e-3)
    }

    @Test
    fun slowDriftIsRejected() {
        // Half the window 0.3° away from the other half: each quarter mean is 0.15° off the overall mean.
        val samples = List(100) { reading(1.0, 0.0) } + List(100) { reading(1.0, 0.3) }
        val result = analyzeWindow(samples, identity)
        assertTrue(result is WindowResult.Moved)
        assertEquals(0.15, (result as WindowResult.Moved).reading.driftDeg, 1e-3)
    }

    @Test
    fun usesThePhoneOrientation() {
        // Phone top edge raised 1° while the top points to the rear: nose down.
        val topEdgeUp = Vec3(0.0, 9.81 * sin(Math.toRadians(1.0)), 9.81 * cos(Math.toRadians(1.0)))
        val r = still(analyzeWindow(List(100) { topEdgeUp }, PhoneOrientation.TOP_TO_REAR))
        assertEquals(-1.0, r.tilt.pitchDeg, 1e-9)
    }

    @Test
    fun uprightPhoneIsNotFlat() {
        val result = analyzeWindow(List(100) { Vec3(0.0, 9.81, 0.0) }, identity)
        assertEquals(WindowResult.NotFlat(90.0), result)
    }

    @Test
    fun faceDownPhoneIsNotFlat() {
        val result = analyzeWindow(List(100) { Vec3(0.0, 0.0, -9.81) }, identity)
        assertEquals(WindowResult.NotFlat(180.0), result)
    }

    @Test
    fun zeroReadingsAreNotFlat() {
        assertTrue(analyzeWindow(List(100) { Vec3(0.0, 0.0, 0.0) }, identity) is WindowResult.NotFlat)
    }

    @Test
    fun tooFewSamplesAreRejected() {
        assertEquals(WindowResult.TooFewSamples(10), analyzeWindow(List(10) { reading(0.0, 0.0) }, identity))
    }

    @Test
    fun limitsAreConfigurable() {
        val samples = List(100) { reading(1.0, 0.0) } + List(100) { reading(1.0, 0.3) }
        assertTrue(analyzeWindow(samples, identity, StillnessLimits(maxDriftDeg = 0.2)) is WindowResult.Still)
    }
}
