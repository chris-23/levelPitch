package io.github.cnissler.levelpitch.ui.level

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.cnissler.levelpitch.LevelPitchApp
import io.github.cnissler.levelpitch.leveling.Wheel
import io.github.cnissler.levelpitch.leveling.WindowResult
import io.github.cnissler.levelpitch.leveling.relativeTo
import io.github.cnissler.levelpitch.profiles.MeasurementRecord
import io.github.cnissler.levelpitch.profiles.sessionReport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** The level loop (F3): measure, place wedges, measure again until level. */
class LevelViewModel(app: LevelPitchApp) : ViewModel() {

    private val repo = app.repository
    private val source = app.accelerometer
    private val running = MutableStateFlow(false)
    private val rejected = MutableStateFlow<WindowResult?>(null)

    val state: StateFlow<LevelUiState> = combine(repo.data, running, rejected) { data, run, rej ->
        levelUiState(data, source.isAvailable, run, rej)
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.Eagerly, levelUiState(repo.data.value, source.isAvailable, false, null))

    fun measure() {
        val s = state.value
        val setup = s.setup as? Setup.Ready ?: return
        if (s.running || !s.sensorAvailable) return
        running.value = true
        rejected.value = null
        viewModelScope.launch {
            try {
                when (val result = source.measure(setup.vehicleProfile.phoneOrientation)) {
                    is WindowResult.Still -> record(result, setup)
                    else -> rejected.value = result
                }
            } finally {
                running.value = false
            }
        }
    }

    private fun record(result: WindowResult.Still, setup: Setup.Ready) {
        val r = result.reading
        val tilt = setup.vehicleProfile.zero?.let { r.tilt.relativeTo(it) } ?: r.tilt
        repo.update { data ->
            data.recordMeasurement(
                MeasurementRecord(
                    timestampMs = System.currentTimeMillis(),
                    pitchDeg = tilt.pitchDeg,
                    rollDeg = tilt.rollDeg,
                    noiseDeg = r.noiseDeg,
                    driftDeg = r.driftDeg,
                    sampleCount = r.sampleCount,
                    wedgeState = data.activeSession?.wedgeState.orEmpty(),
                ),
            )
        }
    }

    /** The user placed the recommended wedges. */
    fun placeRecommended() {
        val plan = state.value.plan ?: return
        repo.update { it.setWedgeState(plan.recommendation.steps) }
    }

    /** The user put [wheel] (and the wheels raised with it) on [step]. */
    fun setStep(wheel: Wheel, step: Int) {
        val setup = state.value.setup as? Setup.Ready ?: return
        repo.update { it.setWedgeState(setup.vehicle.withStep(it.activeSession?.wedgeState.orEmpty(), wheel, step)) }
    }

    /** Caravans: the caravan now stands on its jockey wheel (or, with false, is hitched again). */
    fun setUnhitched(unhitched: Boolean) = repo.update { it.setUnhitched(unhitched) }

    /** JSON of the setup and this pitch's measurements for a tester to send back, or null if nothing measured. */
    fun sessionReport(appVersion: String, device: String): String? =
        sessionReport(repo.data.value, appVersion, device, System.currentTimeMillis())

    /** Start over at a new pitch: no wedges, no measurements. */
    fun newPitch() {
        rejected.value = null
        repo.update { it.newSession() }
    }
}
