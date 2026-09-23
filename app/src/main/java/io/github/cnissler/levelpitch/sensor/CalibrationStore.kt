package io.github.cnissler.levelpitch.sensor

import android.content.Context
import androidx.core.content.edit
import io.github.cnissler.levelpitch.leveling.PhoneOrientation
import io.github.cnissler.levelpitch.leveling.Tilt

/**
 * Phone orientation and one zero offset per orientation (the sensor bias turns with the phone).
 * Interim storage until M3 moves both into the vehicle profile.
 */
class CalibrationStore(context: Context) {

    private val prefs = context.getSharedPreferences("calibration", Context.MODE_PRIVATE)

    var orientation: PhoneOrientation
        get() = prefs.getString(KEY_ORIENTATION, null)
            ?.let { name -> PhoneOrientation.entries.find { it.name == name } }
            ?: PhoneOrientation.TOP_TO_FRONT
        set(value) = prefs.edit { putString(KEY_ORIENTATION, value.name) }

    fun zero(orientation: PhoneOrientation): Tilt? {
        val parts = prefs.getString(zeroKey(orientation), null)?.split(',') ?: return null
        return Tilt(parts[0].toDouble(), parts[1].toDouble())
    }

    fun setZero(orientation: PhoneOrientation, zero: Tilt?) = prefs.edit {
        if (zero == null) remove(zeroKey(orientation)) else putString(zeroKey(orientation), "${zero.pitchDeg},${zero.rollDeg}")
    }

    private fun zeroKey(orientation: PhoneOrientation) = "zero_${orientation.name}"

    private companion object {
        const val KEY_ORIENTATION = "orientation"
    }
}
