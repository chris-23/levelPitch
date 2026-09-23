package io.github.cnissler.levelpitch.profiles

/**
 * Typical caravan sizes for estimated dimensions. Makers publish overall length and width but not
 * track or hitch-to-axle distance, so these are derived by [estimateCaravan], not looked up.
 */
data class CaravanSize(
    val id: String,
    val tandem: Boolean,
    /** Overall length including the drawbar, as in the vehicle papers. */
    val overallLengthMm: Double,
    val widthMm: Double,
) {
    val estimate: CaravanEstimate get() = estimateCaravan(overallLengthMm, widthMm, tandem)
}

data class CaravanEstimate(val trackMm: Double, val hitchToAxleMm: Double, val tandemSpacingMm: Double?)

/** Coupling to front wall. */
private const val DRAWBAR_MM = 1200.0

/** The (mid-)axle sits a little behind the middle of the body, so the nose carries some load. */
private const val AXLE_FRACTION_OF_BODY = 0.52

/** Wheels sit inside the body width; tyre centres are about 15 cm in from each side. */
private const val TRACK_INSET_MM = 300.0

private const val TANDEM_SPACING_MM = 900.0

/**
 * Rough dimensions from overall length and width. Errors of 10 % change the recommended wedge
 * heights by 10 % (about a quarter step at 2° roll), which the re-measure loop corrects.
 */
fun estimateCaravan(overallLengthMm: Double, widthMm: Double, tandem: Boolean): CaravanEstimate {
    val body = overallLengthMm - DRAWBAR_MM
    val hitch = DRAWBAR_MM + AXLE_FRACTION_OF_BODY * body
    return CaravanEstimate(
        trackMm = roundTo10(widthMm - TRACK_INSET_MM),
        hitchToAxleMm = roundTo10(hitch),
        tandemSpacingMm = if (tandem) TANDEM_SPACING_MM else null,
    )
}

private fun roundTo10(mm: Double) = Math.round(mm / 10) * 10.0

/** Single-axle classes first; the default for a new caravan is the medium (or first tandem) one. */
val caravanSizes: List<CaravanSize> = listOf(
    CaravanSize("compact", tandem = false, overallLengthMm = 5500.0, widthMm = 2200.0),
    CaravanSize("medium", tandem = false, overallLengthMm = 6800.0, widthMm = 2300.0),
    CaravanSize("large", tandem = false, overallLengthMm = 7800.0, widthMm = 2500.0),
    CaravanSize("tandem", tandem = true, overallLengthMm = 8500.0, widthMm = 2500.0),
    CaravanSize("tandem-xl", tandem = true, overallLengthMm = 9500.0, widthMm = 2500.0),
)
