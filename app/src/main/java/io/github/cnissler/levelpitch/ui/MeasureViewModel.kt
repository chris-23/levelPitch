package io.github.cnissler.levelpitch.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.cnissler.levelpitch.leveling.PhoneOrientation
import io.github.cnissler.levelpitch.leveling.Tilt
import io.github.cnissler.levelpitch.leveling.WindowResult
import io.github.cnissler.levelpitch.leveling.analyzeWindow
import io.github.cnissler.levelpitch.sensor.AccelerometerSource
import io.github.cnissler.levelpitch.sensor.CalibrationStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class MeasureAction { MEASURE, SET_ZERO }

data class MeasureUiState(
    val sensorAvailable: Boolean,
    val orientation: PhoneOrientation,
    /** Zero offset for [orientation], or null if not calibrated. */
    val zero: Tilt?,
    val running: MeasureAction? = null,
    /** The action that produced [result]. */
    val lastAction: MeasureAction? = null,
    val result: WindowResult? = null,
)

class MeasureViewModel(app: Application) : AndroidViewModel(app) {

    private val source = AccelerometerSource(app)
    private val store = CalibrationStore(app)

    private val _state = MutableStateFlow(
        MeasureUiState(
            sensorAvailable = source.isAvailable,
            orientation = store.orientation,
            zero = store.zero(store.orientation),
        ),
    )
    val state: StateFlow<MeasureUiState> = _state.asStateFlow()

    fun selectOrientation(orientation: PhoneOrientation) {
        if (_state.value.running != null) return
        store.orientation = orientation
        _state.update {
            it.copy(orientation = orientation, zero = store.zero(orientation), lastAction = null, result = null)
        }
    }

    fun measure() = run(MeasureAction.MEASURE)

    /** Measures and stores the raw tilt as zero; only valid while the vehicle is known level. */
    fun setZero() = run(MeasureAction.SET_ZERO)

    fun clearZero() {
        store.setZero(_state.value.orientation, null)
        _state.update { it.copy(zero = null) }
    }

    private fun run(action: MeasureAction) {
        val start = _state.value
        if (start.running != null || !start.sensorAvailable) return
        _state.update { it.copy(running = action, result = null) }
        viewModelScope.launch {
            val result = analyzeWindow(source.collect(SETTLE_MS, WINDOW_MS), start.orientation)
            val newZero = (result as? WindowResult.Still)?.reading?.tilt?.takeIf { action == MeasureAction.SET_ZERO }
            if (newZero != null) store.setZero(start.orientation, newZero)
            _state.update {
                it.copy(running = null, lastAction = action, result = result, zero = newZero ?: it.zero)
            }
        }
    }

    private companion object {
        const val SETTLE_MS = 500L
        const val WINDOW_MS = 2_000L
    }
}
