package io.github.cnissler.levelpitch.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.cnissler.levelpitch.LevelPitchApp
import io.github.cnissler.levelpitch.leveling.PhoneOrientation
import io.github.cnissler.levelpitch.leveling.Tilt
import io.github.cnissler.levelpitch.leveling.WindowResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class MeasureAction { MEASURE, SET_ZERO }

data class CalibrationUiState(
    val sensorAvailable: Boolean,
    /** False until a vehicle is set up; zeros belong to a vehicle. */
    val hasVehicle: Boolean,
    val orientation: PhoneOrientation,
    /** Zero offset for [orientation], or null if not calibrated. */
    val zero: Tilt?,
    val running: MeasureAction? = null,
    /** The action that produced [result]. */
    val lastAction: MeasureAction? = null,
    val result: WindowResult? = null,
)

/** Zero calibration of the active vehicle, plus a raw readout for bench checks. */
class CalibrationViewModel(app: LevelPitchApp) : ViewModel() {

    private val source = app.accelerometer
    private val repo = app.repository

    private data class Run(val running: MeasureAction? = null, val lastAction: MeasureAction? = null, val result: WindowResult? = null)

    private val run = MutableStateFlow(Run())

    val state: StateFlow<CalibrationUiState> = combine(repo.data, run) { data, r ->
        val v = data.activeVehicle
        CalibrationUiState(
            sensorAvailable = source.isAvailable,
            hasVehicle = v != null,
            orientation = v?.phoneOrientation ?: PhoneOrientation.TOP_TO_FRONT,
            zero = v?.zero,
            running = r.running,
            lastAction = r.lastAction,
            result = r.result,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, initial())

    private fun initial(): CalibrationUiState {
        val v = repo.data.value.activeVehicle
        return CalibrationUiState(source.isAvailable, v != null, v?.phoneOrientation ?: PhoneOrientation.TOP_TO_FRONT, v?.zero)
    }

    fun selectOrientation(orientation: PhoneOrientation) {
        if (run.value.running != null) return
        repo.update { it.setOrientation(orientation) }
        run.value = Run()
    }

    fun measure() = start(MeasureAction.MEASURE)

    /** Measures and stores the raw tilt as zero; only valid while the vehicle is known level. */
    fun setZero() = start(MeasureAction.SET_ZERO)

    fun clearZero() = repo.update { it.setZero(state.value.orientation, null) }

    private fun start(action: MeasureAction) {
        val s = state.value
        if (s.running != null || !s.sensorAvailable || !s.hasVehicle) return
        run.update { it.copy(running = action, result = null) }
        viewModelScope.launch {
            val result = source.measure(s.orientation)
            if (action == MeasureAction.SET_ZERO && result is WindowResult.Still) {
                repo.update { it.setZero(s.orientation, result.reading.tilt) }
            }
            run.value = Run(running = null, lastAction = action, result = result)
        }
    }
}
