package io.github.cnissler.levelpitch.profiles

import io.github.cnissler.levelpitch.leveling.Motorhome
import io.github.cnissler.levelpitch.leveling.PhoneOrientation
import io.github.cnissler.levelpitch.leveling.SingleAxleCaravan
import io.github.cnissler.levelpitch.leveling.TandemCaravan
import io.github.cnissler.levelpitch.leveling.Tilt
import io.github.cnissler.levelpitch.leveling.Wheel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfilesTest {

    private val van = VehicleProfile("v", "Van", VehicleType.MOTORHOME_2AXLE, trackMm = 1600.0, wheelbaseMm = 3500.0)

    @Test
    fun mapsEachTypeToItsGeometry() {
        assertEquals(Motorhome(3500.0, 1600.0), van.toVehicle())
        assertEquals(
            SingleAxleCaravan(2000.0, 3000.0),
            VehicleProfile("c", "C", VehicleType.CARAVAN_SINGLE, trackMm = 2000.0, hitchToAxleMm = 3000.0).toVehicle(),
        )
        assertEquals(
            TandemCaravan(2000.0, 900.0, 4000.0),
            VehicleProfile(
                "t", "T", VehicleType.CARAVAN_TANDEM, trackMm = 2000.0, hitchToAxleMm = 4000.0, tandemSpacingMm = 900.0,
            ).toVehicle(),
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun caravanWithoutHitchDistanceIsInvalid() {
        VehicleProfile("c", "C", VehicleType.CARAVAN_SINGLE, trackMm = 2000.0).toVehicle()
    }

    @Test
    fun zeroFollowsTheCurrentOrientation() {
        val v = van.withZero(PhoneOrientation.TOP_TO_FRONT, Tilt(0.3, -0.1))
        assertEquals(Tilt(0.3, -0.1), v.zero)
        assertNull(v.copy(phoneOrientation = PhoneOrientation.TOP_TO_REAR).zero)
        assertNull(v.withZero(PhoneOrientation.TOP_TO_FRONT, null).zero)
    }

    @Test
    fun equipmentValidatesThroughTheEngine() {
        assertEquals(listOf(30.0, 60.0), EquipmentProfile("e", "W", listOf(30.0, 60.0), 2).toEquipment().stepHeightsMm)
    }

    @Test(expected = IllegalArgumentException::class)
    fun decreasingStepsAreInvalid() {
        EquipmentProfile("e", "W", listOf(60.0, 30.0), 2).toEquipment()
    }

    @Test
    fun sessionNoticesWedgeChangesSinceTheLastMeasurement() {
        val m = MeasurementRecord(0, 1.0, 0.0, 0.04, 0.0, 200, wedgeState = mapOf(Wheel.REAR_LEFT to 2))
        val s = LevelSession("s", "v", "e", wedgeState = mapOf(Wheel.REAR_LEFT to 2, Wheel.FRONT_LEFT to 0), measurements = listOf(m))
        assertFalse(s.wedgesChangedSinceMeasurement)
        assertTrue(s.copy(wedgeState = mapOf(Wheel.REAR_LEFT to 1)).wedgesChangedSinceMeasurement)
        assertFalse(LevelSession("s", "v", "e").wedgesChangedSinceMeasurement)
    }
}
