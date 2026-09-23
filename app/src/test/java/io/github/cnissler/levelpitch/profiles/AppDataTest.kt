package io.github.cnissler.levelpitch.profiles

import io.github.cnissler.levelpitch.leveling.PhoneOrientation
import io.github.cnissler.levelpitch.leveling.Tilt
import io.github.cnissler.levelpitch.leveling.Wheel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppDataTest {

    private val van = VehicleProfile("van", "Van", VehicleType.MOTORHOME_2AXLE, trackMm = 1600.0, wheelbaseMm = 3500.0)
    private val bus = van.copy(id = "bus", name = "Bus")
    private val wedges = EquipmentProfile("w", "Wedges", listOf(30.0, 60.0, 90.0), 4)
    private val base = AppData().upsertVehicle(van).upsertEquipment(wedges)
    private val m = MeasurementRecord(1, 1.0, 0.0, 0.04, 0.0, 200, wedgeState = emptyMap())

    @Test
    fun firstProfilesBecomeActive() {
        assertEquals(van, base.activeVehicle)
        assertEquals(wedges, base.activeEquipment)
        assertEquals("van", base.upsertVehicle(bus).activeVehicleId)
    }

    @Test
    fun upsertReplacesById() {
        val d = base.upsertVehicle(van.copy(name = "Renamed"))
        assertEquals(listOf("Renamed"), d.vehicles.map { it.name })
    }

    @Test
    fun deletingTheActiveVehicleActivatesTheNextAndEndsItsSession() {
        val d = base.upsertVehicle(bus).recordMeasurement(m).deleteVehicle("van")
        assertEquals("bus", d.activeVehicleId)
        assertNull(d.session)
        assertNull(AppData().upsertVehicle(van).deleteVehicle("van").activeVehicleId)
    }

    @Test
    fun deletingTheActiveEquipmentEndsItsSession() {
        val d = base.recordMeasurement(m).deleteEquipment("w")
        assertNull(d.activeEquipmentId)
        assertNull(d.session)
    }

    @Test
    fun selectIgnoresUnknownIds() {
        assertEquals(base, base.selectVehicle("nope"))
        assertEquals("bus", base.upsertVehicle(bus).selectVehicle("bus").activeVehicleId)
    }

    @Test
    fun zeroAndOrientationChangeTheActiveVehicle() {
        val d = base.setZero(PhoneOrientation.TOP_TO_LEFT, Tilt(0.2, 0.1)).setOrientation(PhoneOrientation.TOP_TO_LEFT)
        assertEquals(Tilt(0.2, 0.1), d.activeVehicle!!.zero)
        assertNull(d.setZero(PhoneOrientation.TOP_TO_LEFT, null).activeVehicle!!.zero)
    }

    @Test
    fun measurementsAndWedgesStartASessionOnDemand() {
        val d = base.recordMeasurement(m).setWedgeState(mapOf(Wheel.REAR_LEFT to 2, Wheel.FRONT_LEFT to 0))
        val s = d.activeSession!!
        assertEquals(listOf(m), s.measurements)
        assertEquals(mapOf(Wheel.REAR_LEFT to 2), s.wedgeState)
    }

    @Test
    fun switchingVehicleHidesTheOldSession() {
        val d = base.upsertVehicle(bus).recordMeasurement(m).selectVehicle("bus")
        assertNull(d.activeSession)
        assertEquals(listOf(m), d.selectVehicle("van").activeSession!!.measurements)
    }

    @Test
    fun newSessionClearsWedgesAndMeasurements() {
        val d = base.recordMeasurement(m).setWedgeState(mapOf(Wheel.REAR_LEFT to 2)).newSession("fresh")
        assertEquals(LevelSession("fresh", "van", "w"), d.activeSession)
    }

    @Test
    fun changingVehicleTypeOrWedgeStepsEndsTheSession() {
        val d = base.recordMeasurement(m)
        assertEquals(d.session, d.upsertVehicle(van.copy(name = "Renamed", trackMm = 1700.0)).session)
        assertNull(d.upsertVehicle(van.copy(type = VehicleType.CARAVAN_SINGLE, hitchToAxleMm = 3000.0)).session)
        assertEquals(d.session, d.upsertEquipment(wedges.copy(wedgesOwned = 2)).session)
        assertNull(d.upsertEquipment(wedges.copy(stepHeightsMm = listOf(30.0, 60.0))).session)
    }

    @Test
    fun unhitchingOnlyCountsLaterMeasurements() {
        val hitched = base.recordMeasurement(m)
        assertEquals(false, hitched.activeSession!!.unhitched)
        val unhitched = hitched.setUnhitched(true)
        assertEquals(true, unhitched.activeSession!!.unhitched)
        assertEquals(false, unhitched.activeSession!!.measuredSinceUnhitching)
        assertEquals(true, unhitched.recordMeasurement(m).activeSession!!.measuredSinceUnhitching)
        assertEquals(false, unhitched.setUnhitched(false).activeSession!!.unhitched)
        assertEquals(false, unhitched.newSession("n").activeSession!!.unhitched)
    }

    @Test
    fun tutorialIsUnseenUntilMarked() {
        assertEquals(false, AppData().tutorialSeen)
        assertEquals(true, base.markTutorialSeen().tutorialSeen)
        assertEquals(base.vehicles, base.markTutorialSeen().vehicles)
    }

    @Test
    fun noSessionWithoutProfiles() {
        assertEquals(AppData(), AppData().recordMeasurement(m))
    }
}
