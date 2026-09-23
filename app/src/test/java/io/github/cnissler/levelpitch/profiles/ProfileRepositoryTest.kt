package io.github.cnissler.levelpitch.profiles

import io.github.cnissler.levelpitch.leveling.PhoneOrientation
import io.github.cnissler.levelpitch.leveling.Tilt
import io.github.cnissler.levelpitch.leveling.Wheel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ProfileRepositoryTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val van = VehicleProfile("van", "Van", VehicleType.MOTORHOME_2AXLE, trackMm = 1600.0, wheelbaseMm = 3500.0)
    private val wedges = EquipmentProfile("w", "Wedges", listOf(30.0, 60.0, 90.0), 4)

    private fun file() = File(tmp.root, "levelpitch.json")

    @Test
    fun startsEmptyWithoutFile() {
        assertEquals(AppData(), ProfileRepository(file()).data.value)
    }

    @Test
    fun changesSurviveARestart() {
        val repo = ProfileRepository(file())
        repo.update {
            it.upsertVehicle(van).upsertEquipment(wedges)
                .setZero(PhoneOrientation.TOP_TO_FRONT, Tilt(0.3, -0.1))
                .setWedgeState(mapOf(Wheel.REAR_LEFT to 2))
        }
        val reloaded = ProfileRepository(file()).data.value
        assertEquals(repo.data.value, reloaded)
        assertEquals(mapOf(Wheel.REAR_LEFT to 2), reloaded.activeSession!!.wedgeState)
        assertEquals(Tilt(0.3, -0.1), reloaded.activeVehicle!!.zero)
        assertFalse(File(tmp.root, "levelpitch.json.tmp").exists())
    }

    @Test
    fun unchangedDataIsNotWritten() {
        ProfileRepository(file()).update { it }
        assertFalse(file().exists())
    }

    @Test
    fun corruptFileIsKeptAsideAndTheAppStartsEmpty() {
        file().writeText("{ not json")
        val repo = ProfileRepository(file())
        assertEquals(AppData(), repo.data.value)
        assertTrue(File(tmp.root, "levelpitch.json.corrupt").exists())
    }

    @Test
    fun unknownFieldsFromNewerVersionsAreIgnored() {
        file().writeText("""{"version": 2, "futureField": true, "vehicles": []}""")
        assertEquals(AppData(version = 2), ProfileRepository(file()).data.value)
    }

    @Test
    fun enumsAreStoredByName() {
        val repo = ProfileRepository(file())
        repo.update { it.upsertVehicle(van).upsertEquipment(wedges).setWedgeState(mapOf(Wheel.REAR_LEFT to 2)) }
        val text = file().readText()
        assertTrue(text.contains("\"REAR_LEFT\": 2"))
        assertTrue(text.contains("\"TOP_TO_FRONT\""))
    }
}
