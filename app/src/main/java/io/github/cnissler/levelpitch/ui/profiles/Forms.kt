package io.github.cnissler.levelpitch.ui.profiles

import io.github.cnissler.levelpitch.leveling.DEFAULT_TOLERANCE_DEG
import io.github.cnissler.levelpitch.leveling.PhoneOrientation
import io.github.cnissler.levelpitch.profiles.EquipmentProfile
import io.github.cnissler.levelpitch.profiles.VehicleProfile
import io.github.cnissler.levelpitch.profiles.VehicleType
import java.math.BigDecimal

/** Parses a user-typed decimal; accepts a decimal comma ("350,5"). */
fun parseDecimal(text: String): Double? = text.trim().replace(',', '.').toDoubleOrNull()

/** Shortest plain form of [value] for prefilling a text field ("350", "3.5"). */
fun formatDecimal(value: Double): String = BigDecimal.valueOf(value).stripTrailingZeros().toPlainString()

private fun cmToMm(text: String): Double? = parseDecimal(text)?.takeIf { it > 0 }?.let { it * 10 }
private fun mmToCm(mm: Double?): String = mm?.let { formatDecimal(it / 10) } ?: ""

enum class VehicleField { NAME, TRACK, WHEELBASE, HITCH, TANDEM, TOLERANCE }

/** Vehicle editor contents as typed; lengths in cm. */
data class VehicleForm(
    val name: String = "",
    val type: VehicleType = VehicleType.MOTORHOME_2AXLE,
    val trackCm: String = "",
    val wheelbaseCm: String = "",
    val hitchToAxleCm: String = "",
    val tandemSpacingCm: String = "",
    val orientation: PhoneOrientation = PhoneOrientation.TOP_TO_FRONT,
    val toleranceDeg: String = formatDecimal(DEFAULT_TOLERANCE_DEG),
) {
    /** The fields [type] uses; others are ignored. */
    val fields: Set<VehicleField>
        get() = setOf(VehicleField.NAME, VehicleField.TRACK, VehicleField.TOLERANCE) + when (type) {
            VehicleType.MOTORHOME_2AXLE -> setOf(VehicleField.WHEELBASE)
            VehicleType.CARAVAN_SINGLE -> setOf(VehicleField.HITCH)
            VehicleType.CARAVAN_TANDEM -> setOf(VehicleField.HITCH, VehicleField.TANDEM)
        }

    fun errors(): Set<VehicleField> = fields.filterTo(mutableSetOf()) { !isValid(it) }

    private fun isValid(field: VehicleField): Boolean = when (field) {
        VehicleField.NAME -> name.isNotBlank()
        VehicleField.TRACK -> cmToMm(trackCm) != null
        VehicleField.WHEELBASE -> cmToMm(wheelbaseCm) != null
        VehicleField.HITCH -> cmToMm(hitchToAxleCm) != null
        VehicleField.TANDEM -> cmToMm(tandemSpacingCm) != null
        VehicleField.TOLERANCE -> parseDecimal(toleranceDeg)?.let { it in TOLERANCE_RANGE } == true
    }

    /** The profile for this form, keeping [previous]'s zeros; null while the form has errors. */
    fun toProfile(id: String, previous: VehicleProfile? = null): VehicleProfile? {
        if (errors().isNotEmpty()) return null
        fun mm(field: VehicleField, text: String) = if (field in fields) cmToMm(text) else null
        return VehicleProfile(
            id = id,
            name = name.trim(),
            type = type,
            trackMm = cmToMm(trackCm)!!,
            wheelbaseMm = mm(VehicleField.WHEELBASE, wheelbaseCm),
            hitchToAxleMm = mm(VehicleField.HITCH, hitchToAxleCm),
            tandemSpacingMm = mm(VehicleField.TANDEM, tandemSpacingCm),
            phoneOrientation = orientation,
            zeroOffsets = previous?.zeroOffsets.orEmpty(),
            toleranceDeg = parseDecimal(toleranceDeg)!!,
        )
    }

    companion object {
        val TOLERANCE_RANGE = 0.1..5.0

        fun from(p: VehicleProfile) = VehicleForm(
            name = p.name,
            type = p.type,
            trackCm = mmToCm(p.trackMm),
            wheelbaseCm = mmToCm(p.wheelbaseMm),
            hitchToAxleCm = mmToCm(p.hitchToAxleMm),
            tandemSpacingCm = mmToCm(p.tandemSpacingMm),
            orientation = p.phoneOrientation,
            toleranceDeg = formatDecimal(p.toleranceDeg),
        )
    }
}

enum class EquipmentField { NAME, STEPS, WEDGES }

/** Wedge editor contents as typed; step heights in cm, lowest first. */
data class EquipmentForm(
    val name: String = "",
    val stepsCm: List<String> = listOf(""),
    val wedgesOwned: String = "2",
) {
    private val steps: List<Double>? get() = stepsCm.map { cmToMm(it) ?: return null }

    fun errors(): Set<EquipmentField> = buildSet {
        if (name.isBlank()) add(EquipmentField.NAME)
        val s = steps
        if (s == null || s.isEmpty() || s.zipWithNext().any { (a, b) -> b <= a }) add(EquipmentField.STEPS)
        if (wedgesOwned.trim().toIntOrNull()?.let { it >= 1 } != true) add(EquipmentField.WEDGES)
    }

    fun toProfile(id: String): EquipmentProfile? {
        if (errors().isNotEmpty()) return null
        return EquipmentProfile(id, name.trim(), steps!!, wedgesOwned.trim().toInt())
    }

    companion object {
        fun from(p: EquipmentProfile) = EquipmentForm(
            name = p.name,
            stepsCm = p.stepHeightsMm.map { mmToCm(it) },
            wedgesOwned = p.wedgesOwned.toString(),
        )
    }
}
