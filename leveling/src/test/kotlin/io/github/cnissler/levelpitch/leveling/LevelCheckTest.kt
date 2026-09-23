package io.github.cnissler.levelpitch.leveling

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LevelCheckTest {

    private val motorhome = Motorhome(wheelbaseMm = 3500.0, trackMm = 1600.0)
    private val caravan = SingleAxleCaravan(trackMm = 2000.0, hitchToAxleMm = 3000.0)

    @Test
    fun motorhomeUsesTotalTilt() {
        assertTrue(isLevel(motorhome, Tilt(0.3, 0.3)))   // total ≈ 0.42°
        assertFalse(isLevel(motorhome, Tilt(0.4, 0.4)))  // total ≈ 0.57°
        assertTrue(isLevel(motorhome, Tilt(-0.5, 0.0)))
    }

    @Test
    fun caravanNeedsRollAndPitchEachWithinTolerance() {
        assertTrue(isLevel(caravan, Tilt(0.4, -0.4)))
        assertFalse(isLevel(caravan, Tilt(0.0, 0.6)))
        // Roll fine, but the jockey wheel still needs adjusting.
        assertFalse(isLevel(caravan, Tilt(-0.8, 0.0)))
    }

    @Test
    fun toleranceIsConfigurable() {
        assertFalse(isLevel(motorhome, Tilt(0.3, 0.0), toleranceDeg = 0.2))
        assertTrue(isLevel(motorhome, Tilt(0.9, 0.0), toleranceDeg = 1.0))
    }
}
