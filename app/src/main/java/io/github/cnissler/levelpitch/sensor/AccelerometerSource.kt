package io.github.cnissler.levelpitch.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import io.github.cnissler.levelpitch.leveling.PhoneOrientation
import io.github.cnissler.levelpitch.leveling.Vec3
import io.github.cnissler.levelpitch.leveling.WindowResult
import io.github.cnissler.levelpitch.leveling.analyzeWindow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withTimeoutOrNull

/** Raw accelerometer readings (phone frame, m/s², pointing up at rest). */
class AccelerometerSource(context: Context) {

    private val manager = context.getSystemService(SensorManager::class.java)
    private val sensor: Sensor? = manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    val isAvailable: Boolean get() = sensor != null

    private class Sample(val timestampNs: Long, val value: Vec3)

    private fun samples(): Flow<Sample> = callbackFlow {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val (x, y, z) = event.values
                trySend(Sample(event.timestamp, Vec3(x.toDouble(), y.toDouble(), z.toDouble())))
            }

            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) = Unit
        }
        manager.registerListener(listener, sensor, SAMPLING_PERIOD_US)
        awaitClose { manager.unregisterListener(listener) }
    }.buffer(Channel.UNLIMITED)

    /**
     * Readings from [settleMs] to [settleMs] + [durationMs] after the first one, timed by sensor
     * timestamps. The settle time lets the tap on the screen die down. Returns what arrived if the
     * sensor stalls, so the caller's sample-count check rejects it.
     */
    suspend fun collect(settleMs: Long, durationMs: Long): List<Vec3> {
        check(isAvailable) { "no accelerometer" }
        val collected = mutableListOf<Vec3>()
        var firstNs = -1L
        withTimeoutOrNull(settleMs + durationMs + STALL_MARGIN_MS) {
            samples()
                .takeWhile { s ->
                    if (firstNs < 0) firstNs = s.timestampNs
                    s.timestampNs - firstNs < (settleMs + durationMs) * NS_PER_MS
                }
                .filter { s -> s.timestampNs - firstNs >= settleMs * NS_PER_MS }
                .map { it.value }
                .toList(collected)
        }
        return collected
    }

    /** One simple-mode measurement: settle, collect a window, average and check stillness. */
    suspend fun measure(orientation: PhoneOrientation): WindowResult =
        analyzeWindow(collect(SETTLE_MS, WINDOW_MS), orientation)

    private companion object {
        const val SETTLE_MS = 500L
        const val WINDOW_MS = 2_000L

        /** ~100 Hz; above 200 Hz Android 12+ requires HIGH_SAMPLING_RATE_SENSORS. */
        const val SAMPLING_PERIOD_US = 10_000
        const val STALL_MARGIN_MS = 2_000L
        const val NS_PER_MS = 1_000_000L
    }
}
