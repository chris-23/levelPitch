package io.github.cnissler.levelpitch.leveling

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.tan

class VehicleTest {

    @Test
    fun motorhomeWheelsAreCornersAroundMidWheelbase() {
        val v = Motorhome(wheelbaseMm = 3500.0, trackMm = 1600.0)
        assertEquals(Point(1750.0, 800.0), v.wheels[Wheel.FRONT_LEFT])
        assertEquals(Point(1750.0, -800.0), v.wheels[Wheel.FRONT_RIGHT])
        assertEquals(Point(-1750.0, 800.0), v.wheels[Wheel.REAR_LEFT])
        assertEquals(Point(-1750.0, -800.0), v.wheels[Wheel.REAR_RIGHT])
        assertEquals(4, v.wheels.size)
    }

    @Test
    fun motorhomeRaisesEachWheelOnItsOwn() {
        val v = Motorhome(3500.0, 1600.0)
        assertEquals(
            listOf(
                listOf(Wheel.FRONT_LEFT), listOf(Wheel.FRONT_RIGHT),
                listOf(Wheel.REAR_LEFT), listOf(Wheel.REAR_RIGHT),
            ),
            v.raiseGroups,
        )
    }

    @Test
    fun singleAxleCaravanHasTwoWheelsOnTheAxleAndHitchInFront() {
        val v = SingleAxleCaravan(trackMm = 2000.0, hitchToAxleMm = 3000.0)
        assertEquals(mapOf(Wheel.LEFT to Point(0.0, 1000.0), Wheel.RIGHT to Point(0.0, -1000.0)), v.wheels)
        assertEquals(listOf(listOf(Wheel.LEFT), listOf(Wheel.RIGHT)), v.raiseGroups)
        assertEquals(Point(3000.0, 0.0), v.hitch)
    }

    @Test
    fun tandemCaravanRaisesBothWheelsOfASideTogether() {
        val v = TandemCaravan(trackMm = 2000.0, axleSpacingMm = 900.0, hitchToAxleMm = 4000.0)
        assertEquals(Point(450.0, 1000.0), v.wheels[Wheel.FRONT_LEFT])
        assertEquals(Point(-450.0, -1000.0), v.wheels[Wheel.REAR_RIGHT])
        assertEquals(
            listOf(listOf(Wheel.FRONT_LEFT, Wheel.REAR_LEFT), listOf(Wheel.FRONT_RIGHT, Wheel.REAR_RIGHT)),
            v.raiseGroups,
        )
        assertEquals(Point(4000.0, 0.0), v.hitch)
    }

    @Test
    fun contactHeightsFollowTilt() {
        val v = Motorhome(3500.0, 1600.0)
        val h = v.contactHeightsMm(Tilt(pitchDeg = 1.0, rollDeg = 0.0))
        val front = 1750.0 * tan(Math.toRadians(1.0))
        assertEquals(front, h.getValue(Wheel.FRONT_LEFT), 1e-9)
        assertEquals(front, h.getValue(Wheel.FRONT_RIGHT), 1e-9)
        assertEquals(-front, h.getValue(Wheel.REAR_LEFT), 1e-9)
        assertEquals(-front, h.getValue(Wheel.REAR_RIGHT), 1e-9)

        val rolled = v.contactHeightsMm(Tilt(pitchDeg = 0.0, rollDeg = 1.0))
        assertTrue(rolled.getValue(Wheel.FRONT_LEFT) > 0 && rolled.getValue(Wheel.REAR_LEFT) > 0)
        assertTrue(rolled.getValue(Wheel.FRONT_RIGHT) < 0 && rolled.getValue(Wheel.REAR_RIGHT) < 0)
    }

    @Test
    fun caravanWheelHeightsIgnorePitch() {
        val v = SingleAxleCaravan(2000.0, 3000.0)
        val h = v.contactHeightsMm(Tilt(pitchDeg = 2.0, rollDeg = 0.0))
        assertEquals(0.0, h.getValue(Wheel.LEFT), 1e-12)
        assertEquals(0.0, h.getValue(Wheel.RIGHT), 1e-12)
    }

    @Test
    fun withStepSetsTheWholeRaiseGroup() {
        val tandem = TandemCaravan(2000.0, 900.0, 4000.0)
        assertEquals(
            mapOf(Wheel.FRONT_LEFT to 2, Wheel.REAR_LEFT to 2),
            tandem.withStep(emptyMap(), Wheel.REAR_LEFT, 2),
        )
        val van = Motorhome(3500.0, 1600.0)
        assertEquals(
            mapOf(Wheel.FRONT_LEFT to 1, Wheel.REAR_LEFT to 0),
            van.withStep(mapOf(Wheel.FRONT_LEFT to 1, Wheel.REAR_LEFT to 3), Wheel.REAR_LEFT, 0),
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun withStepRejectsForeignWheels() {
        Motorhome(3500.0, 1600.0).withStep(emptyMap(), Wheel.LEFT, 1)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsNonPositiveDimensions() {
        Motorhome(wheelbaseMm = 0.0, trackMm = 1600.0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsNonPositiveHitchDistance() {
        TandemCaravan(trackMm = 2000.0, axleSpacingMm = 900.0, hitchToAxleMm = -1.0)
    }
}
