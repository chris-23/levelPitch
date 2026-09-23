package io.github.cnissler.levelpitch.leveling

import kotlin.math.acos
import kotlin.math.hypot
import kotlin.math.sqrt

/** Thresholds for accepting a measurement window. Defaults are starting points for the bench test. */
data class StillnessLimits(
    /** Largest RMS deviation of single samples from the mean tilt (vibration, tapping). */
    val maxNoiseDeg: Double = 0.5,
    /** Largest deviation of a quarter-window mean from the mean tilt (rocking, slow movement). */
    val maxDriftDeg: Double = 0.1,
    /** Largest angle between the screen normal and vertical for "lying flat, screen up". */
    val maxPhoneTiltDeg: Double = 15.0,
    val minSamples: Int = 50,
)

/** Averaged raw (not zero-corrected) vehicle tilt from one window, with its quality figures. */
data class ImuReading(val tilt: Tilt, val noiseDeg: Double, val driftDeg: Double, val sampleCount: Int)

sealed interface WindowResult {
    data class Still(val reading: ImuReading) : WindowResult
    data class Moved(val reading: ImuReading) : WindowResult
    data class NotFlat(val phoneTiltDeg: Double) : WindowResult
    data class TooFewSamples(val count: Int) : WindowResult
}

/**
 * Averages accelerometer [samples] (phone frame, m/s²) into the raw vehicle tilt and checks that
 * the phone lay flat and still: noise and drift (see [StillnessLimits]) within [limits].
 */
fun analyzeWindow(
    samples: List<Vec3>,
    orientation: PhoneOrientation,
    limits: StillnessLimits = StillnessLimits(),
): WindowResult {
    if (samples.size < limits.minSamples) return WindowResult.TooFewSamples(samples.size)
    val mean = Vec3.mean(samples)
    val phoneTiltDeg = Math.toDegrees(acos((mean.z / mean.norm).coerceIn(-1.0, 1.0)))
    // Negated so that NaN (zero-length mean) also counts as not flat.
    if (!(phoneTiltDeg <= limits.maxPhoneTiltDeg)) return WindowResult.NotFlat(phoneTiltDeg)

    fun tiltOf(v: Vec3) = tiltFromUp(orientation.toVehicle(v))
    val tilt = tiltOf(mean)
    val noise = sqrt(samples.map { distanceDeg(tiltOf(it), tilt).let { d -> d * d } }.average())
    val drift = samples.chunked((samples.size + 3) / 4).maxOf { distanceDeg(tiltOf(Vec3.mean(it)), tilt) }

    val reading = ImuReading(tilt, noise, drift, samples.size)
    return if (noise <= limits.maxNoiseDeg && drift <= limits.maxDriftDeg) {
        WindowResult.Still(reading)
    } else {
        WindowResult.Moved(reading)
    }
}

private fun distanceDeg(a: Tilt, b: Tilt) = hypot(a.pitchDeg - b.pitchDeg, a.rollDeg - b.rollDeg)
