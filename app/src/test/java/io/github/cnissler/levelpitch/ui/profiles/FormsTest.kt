package io.github.cnissler.levelpitch.ui.profiles

import io.github.cnissler.levelpitch.leveling.PhoneOrientation
import io.github.cnissler.levelpitch.leveling.Tilt
import io.github.cnissler.levelpitch.profiles.EquipmentKind
import io.github.cnissler.levelpitch.profiles.EquipmentProfile
import io.github.cnissler.levelpitch.profiles.VehicleProfile
import io.github.cnissler.levelpitch.profiles.vehiclePresets
import io.github.cnissler.levelpitch.profiles.caravanSizes
import io.github.cnissler.levelpitch.profiles.estimateCaravan
import io.github.cnissler.levelpitch.profiles.wedgePresets
import io.github.cnissler.levelpitch.profiles.VehicleType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Locale

class FormsTest {

    @Test
    fun parsesDecimalCommaAndPoint() {
        assertEquals(350.5, parseDecimal(" 350,5 ")!!, 0.0)
        assertEquals(3.5, parseDecimal("3.5")!!, 0.0)
        assertNull(parseDecimal("abc"))
        assertNull(parseDecimal(""))
    }

    @Test
    fun formatsWithoutTrailingZeros() {
        assertEquals("350", formatDecimal(350.0))
        assertEquals("3.5", formatDecimal(3.5))
        assertEquals("0.5", formatDecimal(0.5))
    }

    @Test
    fun displayFormatUsesTheLanguagesDecimalSeparator() {
        assertEquals("403,5", formatDecimal(403.5, Locale.GERMAN))
        assertEquals("403.5", formatDecimal(403.5, Locale.ENGLISH))
        assertEquals("30", formatDecimal(30.0, Locale.GERMAN))
    }

    @Test
    fun motorhomeFormNeedsWheelbaseAndConvertsCmToMm() {
        val form = VehicleForm(name = "Van", trackCm = "160", wheelbaseCm = "350,5")
        assertEquals(emptySet<VehicleField>(), form.errors())
        val p = form.toProfile("id")!!
        assertEquals(1600.0, p.trackMm, 1e-9)
        assertEquals(3505.0, p.wheelbaseMm!!, 1e-9)
        assertNull(p.hitchToAxleMm)
    }

    @Test
    fun onlyTheTypesFieldsAreCheckedAndStored() {
        val form = VehicleForm(
            name = "Caravan", type = VehicleType.CARAVAN_SINGLE, trackCm = "200", wheelbaseCm = "junk", hitchToAxleCm = "",
        )
        assertEquals(setOf(VehicleField.HITCH), form.errors())
        val p = form.copy(hitchToAxleCm = "300").toProfile("id")!!
        assertEquals(3000.0, p.hitchToAxleMm!!, 1e-9)
        assertNull(p.wheelbaseMm)
    }

    @Test
    fun tandemNeedsAxleSpacing() {
        val form = VehicleForm(name = "T", type = VehicleType.CARAVAN_TANDEM, trackCm = "200", hitchToAxleCm = "400")
        assertEquals(setOf(VehicleField.TANDEM), form.errors())
    }

    @Test
    fun rejectsBlankNameNonPositiveLengthsAndOddTolerance() {
        val form = VehicleForm(name = " ", trackCm = "0", wheelbaseCm = "-3", toleranceDeg = "9")
        assertEquals(
            setOf(VehicleField.NAME, VehicleField.TRACK, VehicleField.WHEELBASE, VehicleField.TOLERANCE),
            form.errors(),
        )
        assertNull(form.toProfile("id"))
    }

    @Test
    fun vehicleRoundTripKeepsZeros() {
        val p = VehicleProfile(
            "id", "Van", VehicleType.MOTORHOME_2AXLE, trackMm = 1600.0, wheelbaseMm = 3505.0,
            phoneOrientation = PhoneOrientation.TOP_TO_LEFT, toleranceDeg = 0.3,
        ).withZero(PhoneOrientation.TOP_TO_LEFT, Tilt(0.1, 0.2))
        assertEquals(p, VehicleForm.from(p).toProfile("id", previous = p))
    }

    @Test
    fun presetFillsTypeAndDimensionsButKeepsTheName() {
        val ducato = vehiclePresets.first { it.id == "ducato-camper-chassis" }
        val form = VehicleForm(name = "Mine", type = VehicleType.CARAVAN_SINGLE).withPreset(ducato, ducato.variants[1])
        assertEquals("Mine", form.name)
        assertEquals(VehicleType.MOTORHOME_2AXLE, form.type)
        assertEquals("403.5", form.wheelbaseCm)
        assertEquals("189.5", form.trackCm) // mean of 1810 front and 1980 rear
        assertEquals(ducato.variants[1], form.matchingVariant(ducato))
        assertNull(form.copy(wheelbaseCm = "400").matchingVariant(ducato))
    }

    @Test
    fun everyPresetGivesAValidProfile() {
        for (preset in vehiclePresets) {
            for (variant in preset.variants) {
                val p = VehicleForm(name = "x").withPreset(preset, variant).toProfile("id")
                assertEquals(preset.id, variant.wheelbaseMm, p!!.wheelbaseMm!!, 1e-9)
            }
        }
    }

    @Test
    fun newVehicleStartsFromTheMostCommonBase() {
        val form = VehicleForm.newDefault()
        assertEquals("ducato-camper-chassis", form.presetId)
        assertEquals(setOf(VehicleField.NAME), form.errors())
    }

    @Test
    fun wedgePresetFillsStepsAndOnlyAnEmptyName() {
        val quattro = wedgePresets.first { it.id == "milenco-quattro" }
        val form = EquipmentForm().withPreset(quattro)
        assertEquals(listOf("4", "9", "13", "18"), form.stepsCm)
        assertEquals(quattro.name, form.name)
        assertEquals("Mine", EquipmentForm(name = "Mine").withPreset(quattro).name)
    }

    @Test
    fun autoFilledWedgeNameFollowsTheModel() {
        val thule = wedgePresets.first { it.id == "thule-levelers" }
        assertEquals(thule.name, EquipmentForm.newDefault().withPreset(thule).name)
        val typed = EquipmentForm.newDefault().copy(name = "Yellow ones").withPreset(thule)
        assertEquals("Yellow ones", typed.name)
    }

    @Test
    fun everyWedgePresetGivesAValidProfile() {
        for (preset in wedgePresets) {
            val p = EquipmentForm().withPreset(preset).toProfile("id")
            assertEquals(preset.id, preset.stepHeightsMm, p!!.stepHeightsMm)
        }
    }

    @Test
    fun newWedgesStartFromTheFirstModelAndAreValid() {
        val form = EquipmentForm.newDefault()
        assertEquals("fiamma-level-up", form.presetId)
        assertEquals(emptySet<EquipmentField>(), form.errors())
    }

    @Test
    fun caravanEstimateFollowsLengthAndWidth() {
        val e = estimateCaravan(overallLengthMm = 6800.0, widthMm = 2300.0, tandem = false)
        assertEquals(2000.0, e.trackMm, 0.0)
        assertEquals(4110.0, e.hitchToAxleMm, 0.0) // 1.2 m drawbar + 52 % of 5.6 m body
        assertNull(e.tandemSpacingMm)
        assertEquals(900.0, estimateCaravan(8500.0, 2500.0, tandem = true).tandemSpacingMm!!, 0.0)
    }

    @Test
    fun everyCaravanSizeGivesAValidProfileOfItsType() {
        for (size in caravanSizes) {
            val form = VehicleForm(name = "x").withCaravanSize(size)
            val p = form.toProfile("id")!!
            assertEquals(size.id, if (size.tandem) VehicleType.CARAVAN_TANDEM else VehicleType.CARAVAN_SINGLE, p.type)
            assertEquals(size, form.matchingSize())
            p.toVehicle()
        }
    }

    @Test
    fun switchingToACaravanFillsItsDefaultSize() {
        val caravan = VehicleForm.newDefault().withType(VehicleType.CARAVAN_SINGLE)
        assertEquals("medium", caravan.matchingSize()!!.id)
        val tandem = caravan.withType(VehicleType.CARAVAN_TANDEM)
        assertEquals("tandem", tandem.matchingSize()!!.id)
        assertEquals(VehicleType.MOTORHOME_2AXLE, tandem.withType(VehicleType.MOTORHOME_2AXLE).type)
    }

    @Test
    fun switchingKeepsDimensionsTheUserEntered() {
        val own = VehicleForm(name = "x", type = VehicleType.CARAVAN_SINGLE, trackCm = "205", hitchToAxleCm = "433")
        val tandem = own.withType(VehicleType.CARAVAN_TANDEM)
        assertEquals("205", tandem.trackCm)
        assertEquals("433", tandem.hitchToAxleCm)
    }

    @Test
    fun continuousDevicesNeedAMaximumLiftInsteadOfSteps() {
        val jack = EquipmentForm(name = "Side-lift jack", stepsCm = listOf(""), wedgesOwned = "1", kind = EquipmentKind.CONTINUOUS)
        assertEquals(setOf(EquipmentField.MAX_LIFT), jack.errors())
        assertEquals(setOf(EquipmentField.MAX_LIFT), jack.copy(maxLiftCm = "0,5").errors())
        val p = jack.copy(maxLiftCm = "12").toProfile("id")!!
        assertEquals(EquipmentKind.CONTINUOUS, p.kind)
        assertEquals(120.0, p.maxLiftMm!!, 0.0)
        assertEquals(24, p.toEquipment().stepHeightsMm.size)
        assertEquals(p, EquipmentForm.from(p).toProfile("id"))
    }

    @Test
    fun equipmentStepsMustIncrease() {
        val ok = EquipmentForm(name = "W", stepsCm = listOf("3", "6,5", "9"), wedgesOwned = "4")
        assertEquals(EquipmentProfile("id", "W", listOf(30.0, 65.0, 90.0), 4), ok.toProfile("id"))
        assertEquals(setOf(EquipmentField.STEPS), ok.copy(stepsCm = listOf("6", "3")).errors())
        assertEquals(setOf(EquipmentField.STEPS), ok.copy(stepsCm = listOf("3", "")).errors())
        assertEquals(setOf(EquipmentField.STEPS), ok.copy(stepsCm = emptyList()).errors())
    }

    @Test
    fun equipmentNeedsNameAndAtLeastOneWedge() {
        val form = EquipmentForm(name = "", stepsCm = listOf("3"), wedgesOwned = "0")
        assertEquals(setOf(EquipmentField.NAME, EquipmentField.WEDGES), form.errors())
    }

    @Test
    fun equipmentRoundTrip() {
        val p = EquipmentProfile("id", "W", listOf(30.0, 65.0), 2)
        assertEquals(p, EquipmentForm.from(p).toProfile("id"))
    }
}
