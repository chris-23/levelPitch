package io.github.cnissler.levelpitch.ui.level

import io.github.cnissler.levelpitch.leveling.PhoneOrientation
import io.github.cnissler.levelpitch.leveling.Tilt
import io.github.cnissler.levelpitch.leveling.Wheel
import io.github.cnissler.levelpitch.profiles.AppData
import io.github.cnissler.levelpitch.profiles.EquipmentProfile
import io.github.cnissler.levelpitch.profiles.MeasurementRecord
import io.github.cnissler.levelpitch.profiles.VehicleProfile
import io.github.cnissler.levelpitch.profiles.VehicleType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.atan
import kotlin.math.tan

class LevelStateTest {

    private val van = VehicleProfile("van", "Van", VehicleType.MOTORHOME_2AXLE, trackMm = 1600.0, wheelbaseMm = 3500.0)
    private val tandem = VehicleProfile(
        "t", "Tandem", VehicleType.CARAVAN_TANDEM, trackMm = 2000.0, hitchToAxleMm = 4000.0, tandemSpacingMm = 900.0,
    )
    private val wedges = EquipmentProfile("w", "Wedges", listOf(30.0, 60.0, 90.0), 4)
    private val base = AppData().upsertVehicle(van).upsertEquipment(wedges)

    private fun measured(tilt: Tilt, on: Map<Wheel, Int> = emptyMap()) =
        MeasurementRecord(0, tilt.pitchDeg, tilt.rollDeg, 0.04, 0.0, 200, wedgeState = on)

    private fun state(data: AppData) = levelUiState(data, sensorAvailable = true, running = false, rejected = null)

    @Test
    fun reportsMissingProfiles() {
        assertEquals(Setup.NoVehicle, state(AppData()).setup)
        assertEquals(Setup.NoEquipment, state(AppData().upsertVehicle(van)).setup)
    }

    @Test
    fun reportsProfilesTheEngineRejects() {
        val broken = AppData().upsertVehicle(van.copy(wheelbaseMm = null)).upsertEquipment(wedges)
        assertTrue(state(broken).setup is Setup.Invalid)
    }

    @Test
    fun noPlanBeforeTheFirstMeasurement() {
        val s = state(base)
        assertTrue(s.setup is Setup.Ready)
        assertNull(s.plan)
        assertFalse(s.calibrated)
        assertTrue(state(base.setZero(PhoneOrientation.TOP_TO_FRONT, Tilt.LEVEL)).calibrated)
    }

    @Test
    fun noseUpPlansRearWedges() {
        val plan = state(base.recordMeasurement(measured(Tilt(1.0, 0.0)))).plan!!
        assertFalse(plan.isLevel)
        assertEquals(
            listOf(WedgeChange(listOf(Wheel.REAR_LEFT), 0, 2), WedgeChange(listOf(Wheel.REAR_RIGHT), 0, 2)),
            plan.changes,
        )
        assertFalse(plan.stale)
        assertFalse(plan.applied)
    }

    @Test
    fun placingTheWedgesMarksThePlanAppliedButStale() {
        val d = base.recordMeasurement(measured(Tilt(1.0, 0.0)))
        val placed = d.setWedgeState(state(d).plan!!.recommendation.steps)
        val plan = state(placed).plan!!
        assertTrue(plan.applied)
        assertTrue(plan.stale)
        assertEquals(mapOf(Wheel.REAR_LEFT to 2, Wheel.REAR_RIGHT to 2), state(placed).wedgeState)
    }

    @Test
    fun levelReMeasureStopsTheLoop() {
        val on = mapOf(Wheel.REAR_LEFT to 2, Wheel.REAR_RIGHT to 2)
        val d = base.setWedgeState(on).recordMeasurement(measured(Tilt(0.1, -0.1), on = on))
        val plan = state(d).plan!!
        assertTrue(plan.isLevel)
        assertEquals(emptyList<WedgeChange>(), plan.changes)
        assertFalse(plan.stale)
    }

    @Test
    fun reMeasureOnWrongWedgesPlansTheCorrection() {
        // Nose-up 1° ground, but only step 1 was placed at the rear.
        val on = mapOf(Wheel.REAR_LEFT to 1, Wheel.REAR_RIGHT to 1)
        val remaining = Math.toDegrees(atan((2 * 1750.0 * tan(Math.toRadians(1.0)) - 30.0) / 3500.0))
        val d = base.setWedgeState(on).recordMeasurement(measured(Tilt(remaining, 0.0), on = on))
        assertEquals(
            listOf(WedgeChange(listOf(Wheel.REAR_LEFT), 1, 2), WedgeChange(listOf(Wheel.REAR_RIGHT), 1, 2)),
            state(d).plan!!.changes,
        )
    }

    @Test
    fun tandemChangesMoveBothWheelsOfASide() {
        val d = AppData().upsertVehicle(tandem).upsertEquipment(wedges).recordMeasurement(measured(Tilt(0.0, -1.0)))
        val plan = state(d).plan!!
        assertEquals(listOf(WedgeChange(listOf(Wheel.FRONT_LEFT, Wheel.REAR_LEFT), 0, 1)), plan.changes)
        assertEquals(0.0, plan.recommendation.residualPitchDeg!!, 0.0)
        assertTrue(plan.recommendation.hitchAdjustMm!! > 0)
    }

    private val caravan = VehicleProfile("c", "Caravan", VehicleType.CARAVAN_SINGLE, trackMm = 2000.0, hitchToAxleMm = 4000.0)
    private val caravanBase = AppData().upsertVehicle(caravan).upsertEquipment(wedges)

    @Test
    fun motorhomesHaveNoCaravanSteps() {
        assertNull(state(base.recordMeasurement(measured(Tilt(1.0, 0.0)))).caravanStep)
    }

    @Test
    fun caravanStartsSideToSideAndIgnoresPitchWhileHitched() {
        assertEquals(CaravanStep.SIDE_TO_SIDE, state(caravanBase).caravanStep)
        assertEquals(CaravanStep.SIDE_TO_SIDE, state(caravanBase.recordMeasurement(measured(Tilt(0.0, 1.0)))).caravanStep)
        // Roll level, pitch way off because of the tow car: time to unhitch.
        assertEquals(CaravanStep.UNHITCH, state(caravanBase.recordMeasurement(measured(Tilt(3.0, 0.2)))).caravanStep)
    }

    @Test
    fun wedgesPlacedButNotMeasuredStaySideToSide() {
        val d = caravanBase.recordMeasurement(measured(Tilt(0.0, 1.0)))
        val placed = d.setWedgeState(state(d).plan!!.recommendation.steps)
        assertEquals(CaravanStep.SIDE_TO_SIDE, state(placed).caravanStep)
    }

    @Test
    fun afterUnhitchingOnlyNewMeasurementsCount() {
        val level = caravanBase.recordMeasurement(measured(Tilt(0.1, 0.1)))
        val unhitched = level.setUnhitched(true)
        // The earlier (hitched) measurement was level, but it doesn't count for the jockey wheel.
        assertEquals(CaravanStep.FRONT_TO_BACK, state(unhitched).caravanStep)
        assertEquals(false, state(unhitched).measuredSinceUnhitching)
        val noseUp = unhitched.recordMeasurement(measured(Tilt(1.2, 0.1)))
        assertEquals(CaravanStep.FRONT_TO_BACK, state(noseUp).caravanStep)
        assertTrue(state(noseUp).plan!!.recommendation.hitchAdjustMm!! < 0)
        val done = noseUp.recordMeasurement(measured(Tilt(0.1, 0.1)))
        assertEquals(CaravanStep.STEADIES, state(done).caravanStep)
    }

    @Test
    fun wedgeStateFollowsTheActiveSession() {
        val d = base.setWedgeState(mapOf(Wheel.FRONT_LEFT to 1))
        assertEquals(mapOf(Wheel.FRONT_LEFT to 1), state(d).wedgeState)
        assertEquals(emptyMap<Wheel, Int>(), state(d.newSession("n")).wedgeState)
    }
}
