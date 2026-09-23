package io.github.cnissler.levelpitch.leveling

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.atan
import kotlin.math.tan

class RecommenderTest {

    private val wedges = Equipment(stepHeightsMm = listOf(30.0, 60.0, 90.0), wedgesOwned = 4)
    private val motorhome = Motorhome(wheelbaseMm = 3500.0, trackMm = 1600.0)
    private val caravan = SingleAxleCaravan(trackMm = 2000.0, hitchToAxleMm = 3000.0)
    private val tandem = TandemCaravan(trackMm = 2000.0, axleSpacingMm = 900.0, hitchToAxleMm = 4000.0)

    private fun tilt(pitchDeg: Double, rollDeg: Double) = Measurement.VehicleTilt(Tilt(pitchDeg, rollDeg))
    private fun ground(vararg heights: Pair<Wheel, Double>) = Measurement.GroundHeights(mapOf(*heights))
    private fun deg(slope: Double) = Math.toDegrees(atan(slope))
    private fun rise(mm: Double, deg: Double) = mm * tan(Math.toRadians(deg))

    // --- Equipment ---

    @Test
    fun stepZeroIsNoWedge() {
        assertEquals(0.0, wedges.heightMm(0), 0.0)
        assertEquals(60.0, wedges.heightMm(2), 0.0)
        assertEquals(0..3, wedges.steps)
    }

    @Test(expected = IllegalArgumentException::class)
    fun stepHeightsMustIncrease() {
        Equipment(listOf(30.0, 30.0), wedgesOwned = 2)
    }

    @Test(expected = IllegalArgumentException::class)
    fun stepHeightsMustBePositive() {
        Equipment(listOf(0.0, 30.0), wedgesOwned = 2)
    }

    // --- Motorhome ---

    @Test
    fun levelVehicleNeedsNoWedges() {
        val r = recommend(motorhome, wedges, tilt(0.0, 0.0))
        assertEquals(motorhome.wheels.keys.associateWith { 0 }, r.steps)
        assertEquals(0.0, r.residualDeg, 1e-12)
        assertTrue(r.isWithin())
        assertNull(r.hitchAdjustMm)
    }

    @Test
    fun noseUpRaisesTheRearWheels() {
        val r = recommend(motorhome, wedges, tilt(1.0, 0.0))
        assertEquals(
            mapOf(Wheel.FRONT_LEFT to 0, Wheel.FRONT_RIGHT to 0, Wheel.REAR_LEFT to 2, Wheel.REAR_RIGHT to 2),
            r.steps,
        )
        // Front-to-rear difference 2·1750·tan 1° ≈ 61.1 mm; 60 mm leaves ≈ 1.1 mm.
        val left = 2 * rise(1750.0, 1.0) - 60.0
        assertEquals(deg(left / 3500.0), r.residualPitchDeg!!, 1e-9)
        assertEquals(0.0, r.residualRollDeg, 1e-9)
        assertTrue(r.isWithin(0.05))
    }

    @Test
    fun leftSideUpRaisesTheRightWheels() {
        val r = recommend(motorhome, wedges, tilt(0.0, 1.0))
        assertEquals(
            mapOf(Wheel.FRONT_LEFT to 0, Wheel.FRONT_RIGHT to 1, Wheel.REAR_LEFT to 0, Wheel.REAR_RIGHT to 1),
            r.steps,
        )
        assertEquals(deg((2 * rise(800.0, 1.0) - 30.0) / 1600.0), r.residualRollDeg, 1e-9)
    }

    @Test
    fun tooSteepReportsBestAchievableResidual() {
        val r = recommend(motorhome, wedges, tilt(5.0, 0.0))
        assertEquals(
            mapOf(Wheel.FRONT_LEFT to 0, Wheel.FRONT_RIGHT to 0, Wheel.REAR_LEFT to 3, Wheel.REAR_RIGHT to 3),
            r.steps,
        )
        assertEquals(deg((2 * rise(1750.0, 5.0) - 90.0) / 3500.0), r.residualDeg, 1e-9)
        assertFalse(r.isWithin())
    }

    @Test
    fun respectsNumberOfWedgesOwned() {
        val all = recommend(motorhome, wedges, tilt(0.0, 1.0))
        val one = recommend(motorhome, wedges.copy(wedgesOwned = 1), tilt(0.0, 1.0))
        assertEquals(1, one.wedgeCount)
        assertTrue(one.residualDeg > all.residualDeg)

        val none = recommend(motorhome, wedges.copy(wedgesOwned = 0), tilt(0.0, 1.0))
        assertEquals(0, none.wedgeCount)
    }

    @Test
    fun usesGroundHeights() {
        val r = recommend(
            motorhome, wedges,
            ground(
                Wheel.FRONT_LEFT to 0.0, Wheel.FRONT_RIGHT to 0.0,
                Wheel.REAR_LEFT to -60.0, Wheel.REAR_RIGHT to -60.0,
            ),
        )
        assertEquals(mapOf(Wheel.FRONT_LEFT to 0, Wheel.FRONT_RIGHT to 0, Wheel.REAR_LEFT to 2, Wheel.REAR_RIGHT to 2), r.steps)
        assertEquals(0.0, r.residualDeg, 1e-12)
    }

    @Test(expected = IllegalArgumentException::class)
    fun groundHeightsMustCoverEveryWheel() {
        recommend(motorhome, wedges, ground(Wheel.FRONT_LEFT to 0.0, Wheel.FRONT_RIGHT to 0.0))
    }

    // --- Single-axle caravan ---

    @Test
    fun caravanLevelsRollWithWedgesAndPitchWithJockeyWheel() {
        val r = recommend(caravan, wedges, tilt(0.0, 1.0))
        assertEquals(mapOf(Wheel.LEFT to 0, Wheel.RIGHT to 1), r.steps)
        assertEquals(deg((2 * rise(1000.0, 1.0) - 30.0) / 2000.0), r.residualRollDeg, 1e-9)
        assertEquals(r.residualRollDeg, r.residualDeg, 0.0)
        // One side up 30 mm lifts the axle centre 15 mm, so the hitch must follow.
        assertEquals(15.0, r.hitchAdjustMm!!, 1e-9)
        assertEquals(0.0, r.residualPitchDeg!!, 0.0)
    }

    @Test
    fun caravanNoseUpLowersTheHitch() {
        val r = recommend(caravan, wedges, tilt(1.0, 0.0))
        assertEquals(0, r.wedgeCount)
        assertEquals(-rise(3000.0, 1.0), r.hitchAdjustMm!!, 1e-9)
        assertEquals(0.0, r.residualDeg, 1e-12)
    }

    @Test
    fun caravanGroundHeightsGiveNoJockeyAdvice() {
        val r = recommend(caravan, wedges, ground(Wheel.LEFT to 0.0, Wheel.RIGHT to -30.0))
        assertEquals(mapOf(Wheel.LEFT to 0, Wheel.RIGHT to 1), r.steps)
        assertNull(r.hitchAdjustMm)
        assertNull(r.residualPitchDeg)
    }

    @Test
    fun equalResidualPrefersTheLowerWedge() {
        // Right side 45 mm low: 30 mm and 60 mm both leave 15 mm.
        val r = recommend(caravan, wedges, ground(Wheel.LEFT to 0.0, Wheel.RIGHT to -45.0))
        assertEquals(mapOf(Wheel.LEFT to 0, Wheel.RIGHT to 1), r.steps)
    }

    @Test
    fun negligibleImprovementIsNotWorthAWedge() {
        // 15.1 mm low: a 30 mm wedge leaves 14.9 mm, under 0.01° better than doing nothing.
        val r = recommend(caravan, wedges, ground(Wheel.LEFT to 0.0, Wheel.RIGHT to -15.1))
        assertEquals(0, r.wedgeCount)
    }

    // --- Tandem caravan ---

    @Test
    fun tandemRaisesBothWheelsOfTheLowSide() {
        val r = recommend(tandem, wedges, tilt(0.0, -1.0))
        assertEquals(
            mapOf(Wheel.FRONT_LEFT to 1, Wheel.REAR_LEFT to 1, Wheel.FRONT_RIGHT to 0, Wheel.REAR_RIGHT to 0),
            r.steps,
        )
        assertEquals(2, r.wedgeCount)
        assertEquals(15.0, r.hitchAdjustMm!!, 1e-9)
    }

    @Test
    fun tandemNeedsTwoWedgesToRaiseASide() {
        val r = recommend(tandem, wedges.copy(wedgesOwned = 1), tilt(0.0, -1.0))
        assertEquals(0, r.wedgeCount)
    }
}
