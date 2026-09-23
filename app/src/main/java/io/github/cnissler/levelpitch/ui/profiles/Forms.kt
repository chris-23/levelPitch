package io.github.cnissler.levelpitch.ui.profiles

import io.github.cnissler.levelpitch.leveling.DEFAULT_TOLERANCE_DEG
import io.github.cnissler.levelpitch.leveling.PhoneOrientation
import io.github.cnissler.levelpitch.profiles.CaravanSize
import io.github.cnissler.levelpitch.profiles.EquipmentKind
import io.github.cnissler.levelpitch.profiles.EquipmentProfile
import io.github.cnissler.levelpitch.profiles.caravanSizes
import io.github.cnissler.levelpitch.profiles.PresetVariant
import io.github.cnissler.levelpitch.profiles.VehiclePreset
import io.github.cnissler.levelpitch.profiles.WedgePreset
import io.github.cnissler.levelpitch.profiles.wedgePresets
import io.github.cnissler.levelpitch.profiles.vehiclePresets
import io.github.cnissler.levelpitch.profiles.VehicleProfile
import io.github.cnissler.levelpitch.profiles.VehicleType
import java.math.BigDecimal
import java.text.DecimalFormatSymbols
import java.util.Locale

/** Parses a user-typed decimal; accepts a decimal comma ("350,5"). */
fun parseDecimal(text: String): Double? = text.trim().replace(',', '.').toDoubleOrNull()

/** Shortest plain form of [value] for prefilling a text field ("350", "3.5"). */
fun formatDecimal(value: Double): String = BigDecimal.valueOf(value).stripTrailingZeros().toPlainString()

/** Like [formatDecimal], with [locale]'s decimal separator for display ("3,5" in German). */
fun formatDecimal(value: Double, locale: Locale): String =
    formatDecimal(value).replace('.', DecimalFormatSymbols.getInstance(locale).decimalSeparator)

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
    /** Base vehicle the dimensions were taken from; only a hint for the editor, not stored. */
    val presetId: String? = null,
) {
    /** Fills type and dimensions from a base vehicle; name, orientation and tolerance stay. */
    fun withPreset(preset: VehiclePreset, variant: PresetVariant) = copy(
        type = VehicleType.MOTORHOME_2AXLE,
        wheelbaseCm = mmToCm(variant.wheelbaseMm),
        trackCm = mmToCm(variant.trackMm),
        presetId = preset.id,
    )

    /** Estimated caravan dimensions for a typical size; the type follows the size's axle count. */
    fun withCaravanSize(size: CaravanSize): VehicleForm {
        val e = size.estimate
        return copy(
            type = if (size.tandem) VehicleType.CARAVAN_TANDEM else VehicleType.CARAVAN_SINGLE,
            trackCm = mmToCm(e.trackMm),
            hitchToAxleCm = mmToCm(e.hitchToAxleMm),
            tandemSpacingCm = e.tandemSpacingMm?.let { mmToCm(it) } ?: tandemSpacingCm,
            presetId = size.id,
        )
    }

    /** The caravan size the current dimensions match, if any. */
    fun matchingSize(): CaravanSize? = caravanSizes.find { s ->
        val e = s.estimate
        type == (if (s.tandem) VehicleType.CARAVAN_TANDEM else VehicleType.CARAVAN_SINGLE) &&
            parseDecimal(trackCm) == e.trackMm / 10 && parseDecimal(hitchToAxleCm) == e.hitchToAxleMm / 10 &&
            (!s.tandem || parseDecimal(tandemSpacingCm) == e.tandemSpacingMm!! / 10)
    }

    /**
     * Switches the type. A caravan without a hitch distance yet, or one still on a typical size,
     * gets the default size for the new type, so every type starts with usable values.
     */
    fun withType(t: VehicleType): VehicleForm {
        val switched = copy(type = t)
        if (t == VehicleType.MOTORHOME_2AXLE) return switched
        val untouched = hitchToAxleCm.isBlank() || matchingSize() != null
        return if (untouched) switched.withCaravanSize(defaultCaravanSize(t)) else switched
    }

    /** The preset variant the current dimensions match, if any. */
    fun matchingVariant(preset: VehiclePreset): PresetVariant? = preset.variants.find {
        type == VehicleType.MOTORHOME_2AXLE &&
            parseDecimal(wheelbaseCm) == it.wheelbaseMm / 10 && parseDecimal(trackCm) == it.trackMm / 10
    }

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

        fun defaultCaravanSize(type: VehicleType): CaravanSize =
            caravanSizes.first { it.id == if (type == VehicleType.CARAVAN_TANDEM) "tandem" else "medium" }

        /** A new vehicle starts from the most common base: the Ducato motorhome chassis, long wheelbase. */
        fun newDefault(): VehicleForm = vehiclePresets.first().let { VehicleForm().withPreset(it, it.variants.last()) }

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

enum class EquipmentField { NAME, STEPS, MAX_LIFT, WEDGES }

/** Wedge editor contents as typed; step heights in cm, lowest first. */
data class EquipmentForm(
    val name: String = "",
    val stepsCm: List<String> = listOf(""),
    val wedgesOwned: String = "2",
    val kind: EquipmentKind = EquipmentKind.STEPPED,
    val maxLiftCm: String = "",
    /** Wedge model the steps were taken from; only a hint for the editor, not stored. */
    val presetId: String? = null,
) {
    /** Fills the steps from a wedge model, and the name unless the user typed one. */
    fun withPreset(preset: WedgePreset) = copy(
        name = if (name.isBlank() || name == wedgePresets.find { it.id == presetId }?.name) preset.name else name,
        stepsCm = preset.stepHeightsMm.map { mmToCm(it) },
        presetId = preset.id,
    )

    private val steps: List<Double>? get() = stepsCm.map { cmToMm(it) ?: return null }

    private val maxLiftMm: Double? get() = cmToMm(maxLiftCm)?.takeIf { it >= MIN_LIFT_MM }

    fun errors(): Set<EquipmentField> = buildSet {
        if (name.isBlank()) add(EquipmentField.NAME)
        if (kind == EquipmentKind.STEPPED) {
            val s = steps
            if (s == null || s.isEmpty() || s.zipWithNext().any { (a, b) -> b <= a }) add(EquipmentField.STEPS)
        } else if (maxLiftMm == null) {
            add(EquipmentField.MAX_LIFT)
        }
        if (wedgesOwned.trim().toIntOrNull()?.let { it >= 1 } != true) add(EquipmentField.WEDGES)
    }

    fun toProfile(id: String): EquipmentProfile? {
        if (errors().isNotEmpty()) return null
        val owned = wedgesOwned.trim().toInt()
        return when (kind) {
            EquipmentKind.STEPPED -> EquipmentProfile(id, name.trim(), steps!!, owned)
            EquipmentKind.CONTINUOUS ->
                EquipmentProfile(id, name.trim(), emptyList(), owned, EquipmentKind.CONTINUOUS, maxLiftMm)
        }
    }

    companion object {
        /** New wedges start from the first (most widespread) model. */
        fun newDefault(): EquipmentForm = EquipmentForm().withPreset(wedgePresets.first())

        /** Below one resolution step a continuous device can't be modelled. */
        const val MIN_LIFT_MM = 10.0

        fun from(p: EquipmentProfile) = EquipmentForm(
            name = p.name,
            stepsCm = p.stepHeightsMm.map { mmToCm(it) }.ifEmpty { listOf("") },
            wedgesOwned = p.wedgesOwned.toString(),
            kind = p.kind,
            maxLiftCm = mmToCm(p.maxLiftMm),
        )
    }
}
