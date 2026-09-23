package io.github.cnissler.levelpitch

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.github.cnissler.levelpitch.ui.LevelPitchNavHost
import io.github.cnissler.levelpitch.ui.theme.LevelPitchTheme

/** AppCompatActivity so that the per-app language (AppCompatDelegate) applies on every Android version. */
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LevelPitchTheme {
                LevelPitchNavHost()
            }
        }
    }
}
