package io.github.cnissler.levelpitch.profiles

import io.github.cnissler.levelpitch.leveling.Wheel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SessionReportTest {

    private val van = VehicleProfile("van", "Van", VehicleType.MOTORHOME_2AXLE, trackMm = 1600.0, wheelbaseMm = 3500.0)
    private val wedges = EquipmentProfile("w", "Wedges", listOf(30.0, 60.0, 90.0), 4)
    private val base = AppData().upsertVehicle(van).upsertEquipment(wedges)
    private val m = MeasurementRecord(1, 1.0, 0.2, 0.04, 0.01, 200, wedgeState = emptyMap())

    @Test
    fun nothingToReportWithoutMeasurements() {
        assertNull(sessionReport(AppData(), "0.3.0", "Pixel", 0))
        assertNull(sessionReport(base, "0.3.0", "Pixel", 0))
    }

    @Test
    fun reportRoundTripsWithSetupAndSession() {
        val data = base.recordMeasurement(m).setWedgeState(mapOf(Wheel.REAR_LEFT to 2))
        val json = sessionReport(data, "0.3.0", "Google Pixel 10 Pro, Android 17", 1234)!!
        val report = appDataJson.decodeFromString(SessionReport.serializer(), json)
        assertEquals("0.3.0", report.appVersion)
        assertEquals("Google Pixel 10 Pro, Android 17", report.device)
        assertEquals(van, report.vehicle)
        assertEquals(wedges, report.equipment)
        assertEquals(listOf(m), report.session.measurements)
        assertEquals(mapOf(Wheel.REAR_LEFT to 2), report.session.wedgeState)
    }
}
