package io.github.cnissler.levelpitch

import android.app.Application
import io.github.cnissler.levelpitch.profiles.ProfileRepository
import io.github.cnissler.levelpitch.sensor.AccelerometerSource
import java.io.File

/** App-wide singletons. */
class LevelPitchApp : Application() {
    val repository: ProfileRepository by lazy { ProfileRepository(File(filesDir, "levelpitch.json")) }
    val accelerometer: AccelerometerSource by lazy { AccelerometerSource(this) }
}
