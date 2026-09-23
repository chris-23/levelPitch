package io.github.cnissler.levelpitch.profiles

/** One wheelbase variant of a base vehicle, with its front and rear track in mm. */
data class PresetVariant(val wheelbaseMm: Double, val frontTrackMm: Double, val rearTrackMm: Double) {
    /** The engine models one track width; front and rear differ by a few %, so use their mean. */
    val trackMm: Double get() = (frontTrackMm + rearTrackMm) / 2
}

/**
 * A common motorhome base vehicle. Values are manufacturer figures collected in September 2026;
 * [approximate] marks tracks that could not be confirmed in a spec sheet. Converters sometimes
 * change wheelbase or track, so the editor says to check the vehicle papers.
 */
data class VehiclePreset(
    val id: String,
    val name: String,
    val variants: List<PresetVariant>,
    val approximate: Boolean = false,
)

/** Sorted by how common the base is under European motorhomes (Ducato platform ≈ 2/3 of the market). */
val vehiclePresets: List<VehiclePreset> = listOf(
    // Fiat camper chassis: rear track widened by 190 mm for motorhome bodies.
    VehiclePreset(
        "ducato-camper-chassis",
        "Fiat Ducato / Citroën Jumper / Peugeot Boxer – motorhome chassis",
        listOf(PresetVariant(3450.0, 1810.0, 1980.0), PresetVariant(4035.0, 1810.0, 1980.0)),
    ),
    VehiclePreset(
        "ducato-van",
        "Fiat Ducato / Citroën Jumper / Peugeot Boxer / Opel Movano – van",
        listOf(
            PresetVariant(3000.0, 1810.0, 1790.0),
            PresetVariant(3450.0, 1810.0, 1790.0),
            PresetVariant(4035.0, 1810.0, 1790.0),
        ),
    ),
    VehiclePreset(
        "sprinter-2018",
        "Mercedes-Benz Sprinter (2018+)",
        listOf(
            PresetVariant(3250.0, 1759.0, 1770.0),
            PresetVariant(3665.0, 1759.0, 1770.0),
            PresetVariant(4325.0, 1759.0, 1770.0),
        ),
    ),
    VehiclePreset(
        "crafter-2017",
        "VW Crafter / MAN TGE (2017+)",
        listOf(PresetVariant(3640.0, 1770.0, 1784.0), PresetVariant(4490.0, 1770.0, 1784.0)),
    ),
    VehiclePreset(
        "transit-2014",
        "Ford Transit (2014+)",
        listOf(PresetVariant(3300.0, 1732.0, 1759.0), PresetVariant(3750.0, 1732.0, 1743.0)),
    ),
    VehiclePreset(
        "vw-t6",
        "VW T6 / T6.1 (Transporter, Multivan, California)",
        listOf(PresetVariant(3000.0, 1630.0, 1630.0), PresetVariant(3400.0, 1630.0, 1630.0)),
        approximate = true,
    ),
)
