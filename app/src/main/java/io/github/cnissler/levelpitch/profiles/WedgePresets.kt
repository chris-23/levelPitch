package io.github.cnissler.levelpitch.profiles

/** A common stepped wedge model; heights are the lift of each step as published (September 2026). */
data class WedgePreset(val id: String, val name: String, val stepHeightsMm: List<Double>)

/** Usually sold as pairs, so the editor keeps its default of 2 wedges. */
val wedgePresets: List<WedgePreset> = listOf(
    WedgePreset("fiamma-level-up", "Fiamma Level Up / Berger Multi Level Ramp", listOf(40.0, 70.0, 100.0)),
    WedgePreset("milenco-quattro", "Milenco Quattro 3 / Quattro Huge", listOf(40.0, 90.0, 130.0, 180.0)),
    WedgePreset("thule-levelers", "Thule Levelers", listOf(44.0, 78.0, 112.0)),
    WedgePreset("froli-stufenkeil", "Froli Stufenkeil", listOf(45.0, 75.0, 105.0)),
)
