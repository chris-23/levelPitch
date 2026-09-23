package io.github.cnissler.levelpitch.leveling

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.atan

class RemeasureTest {

    private val wedges = Equipment(stepHeightsMm = listOf(30.0, 60.0, 90.0), wedgesOwned = 4)
    private val motorhome = Motorhome(wheelbaseMm = 3500.0, trackMm = 1600.0)
    private val caravan = SingleAxleCaravan(trackMm = 2000.0, hitchToAxleMm = 3000.0)

    /** What the phone reads when the motorhome stands on [state] on ground tilted by [groundTilt]. */
    private fun measuredOn(groundTilt: Tilt, state: WedgeState): Measurement.VehicleTilt {
        val raised = motorhome.contactHeightsMm(groundTilt)
            .mapValues { (wheel, z) -> z + wedges.heightMm(state[wheel] ?: 0) }
        val s = fitSlopes(motorhome.wheels.map { (wheel, p) -> p to raised.getValue(wheel) })
        return Measurement.VehicleTilt(Tilt.fromSlopes(s.x!!, s.y!!))
    }

    @Test
    fun afterFollowingTheAdviceNothingChanges() {
        val ground = Tilt(pitchDeg = 1.0, rollDeg = 0.3)
        val first = recommend(motorhome, wedges, Measurement.VehicleTilt(ground))
        val again = recommend(motorhome, wedges, measuredOn(ground, first.steps), current = first.steps)
        assertEquals(first.steps, again.steps)
        assertEquals(emptyMap<Wheel, Int>(), again.changesFrom(first.steps))
        assertEquals(first.residualDeg, again.residualDeg, 1e-9)
    }

    @Test
    fun tooLowWedgesGoOneStepHigher() {
        val ground = Tilt(pitchDeg = 1.0, rollDeg = 0.0)
        val current = mapOf(Wheel.REAR_LEFT to 1, Wheel.REAR_RIGHT to 1)
        val r = recommend(motorhome, wedges, measuredOn(ground, current), current)
        assertEquals(mapOf(Wheel.REAR_LEFT to 1, Wheel.REAR_RIGHT to 1), r.changesFrom(current))
        assertEquals(2, r.steps[Wheel.REAR_LEFT])
    }

    @Test
    fun unnecessaryWedgeIsRemoved() {
        val current = mapOf(Wheel.FRONT_LEFT to 1)
        val r = recommend(motorhome, wedges, measuredOn(Tilt.LEVEL, current), current)
        assertEquals(0, r.wedgeCount)
        assertEquals(mapOf(Wheel.FRONT_LEFT to -1), r.changesFrom(current))
    }

    @Test
    fun equallyGoodCurrentStateIsKept() {
        // All four on step 1 is as level as no wedges; don't make the user drive off them.
        val current = motorhome.wheels.keys.associateWith { 1 }
        val r = recommend(motorhome, wedges, measuredOn(Tilt.LEVEL, current), current)
        assertEquals(current, r.steps)
    }

    @Test
    fun caravanHitchCorrectionAccountsForWedgesAlreadyInPlace() {
        // Ground rolled 1° left-up; right wheel already on step 1, which lifted the axle centre
        // 15 mm while the hitch stayed put, so the caravan now points nose down.
        val current = mapOf(Wheel.RIGHT to 1)
        val first = recommend(caravan, wedges, Measurement.VehicleTilt(Tilt(0.0, 1.0)))
        val noseDown = Math.toDegrees(atan(-15.0 / 3000.0))
        val r = recommend(
            caravan, wedges, Measurement.VehicleTilt(Tilt(noseDown, first.residualRollDeg)), current,
        )
        assertEquals(current + (Wheel.LEFT to 0), r.steps)
        assertEquals(15.0, r.hitchAdjustMm!!, 1e-9)

        // After raising the hitch: level pitch, nothing left to do.
        val done = recommend(
            caravan, wedges, Measurement.VehicleTilt(Tilt(0.0, first.residualRollDeg)), current,
        )
        assertEquals(emptyMap<Wheel, Int>(), done.changesFrom(current))
        assertEquals(0.0, done.hitchAdjustMm!!, 1e-9)
    }

    @Test(expected = IllegalArgumentException::class)
    fun currentStateMustUseTheVehiclesWheels() {
        recommend(motorhome, wedges, Measurement.VehicleTilt(Tilt.LEVEL), current = mapOf(Wheel.LEFT to 1))
    }

    @Test(expected = IllegalArgumentException::class)
    fun currentStateMustUseExistingSteps() {
        recommend(motorhome, wedges, Measurement.VehicleTilt(Tilt.LEVEL), current = mapOf(Wheel.FRONT_LEFT to 4))
    }
}
