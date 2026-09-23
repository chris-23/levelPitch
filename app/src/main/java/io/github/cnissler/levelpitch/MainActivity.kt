package io.github.cnissler.levelpitch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.github.cnissler.levelpitch.ui.LevelPitchNavHost
import io.github.cnissler.levelpitch.ui.theme.LevelPitchTheme

class MainActivity : ComponentActivity() {
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
