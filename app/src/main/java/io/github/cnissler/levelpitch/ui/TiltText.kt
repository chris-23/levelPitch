package io.github.cnissler.levelpitch.ui

import kotlin.math.roundToLong

/** Angles are shown with two decimals. */
private const val DISPLAY_STEPS_PER_DEG = 100.0

/** [deg] rounded to the displayed precision, without a negative zero ("-0.00°"). */
fun displayDeg(deg: Double): Double = (deg * DISPLAY_STEPS_PER_DEG).roundToLong() / DISPLAY_STEPS_PER_DEG + 0.0

enum class Direction { POSITIVE, NEGATIVE, NONE }

/** Sign of [deg] as displayed, so "+0.00°" never gets a direction word. */
fun direction(deg: Double): Direction {
    val shown = displayDeg(deg)
    return when {
        shown > 0 -> Direction.POSITIVE
        shown < 0 -> Direction.NEGATIVE
        else -> Direction.NONE
    }
}
