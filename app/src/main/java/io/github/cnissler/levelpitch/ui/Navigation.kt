package io.github.cnissler.levelpitch.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import io.github.cnissler.levelpitch.LevelPitchApp
import io.github.cnissler.levelpitch.ui.level.LevelScreen
import io.github.cnissler.levelpitch.ui.profiles.EquipmentEditScreen
import io.github.cnissler.levelpitch.ui.profiles.ProfilesScreen
import io.github.cnissler.levelpitch.ui.profiles.VehicleEditScreen
import io.github.cnissler.levelpitch.ui.tutorial.TutorialScreen
import kotlinx.serialization.Serializable

@Serializable object HomeRoute
@Serializable object CalibrationRoute
@Serializable object SettingsRoute
@Serializable object TutorialRoute
@Serializable object ProfilesRoute
@Serializable data class VehicleEditRoute(val id: String? = null)
@Serializable data class EquipmentEditRoute(val id: String? = null)

@Composable
fun LevelPitchNavHost() {
    val nav = rememberNavController()
    val repo = (LocalContext.current.applicationContext as LevelPitchApp).repository
    // First start: the tutorial comes first; afterwards it is in the menu.
    val start: Any = remember { if (repo.data.value.tutorialSeen) HomeRoute else TutorialRoute }
    NavHost(nav, startDestination = start) {
        composable<TutorialRoute> {
            TutorialScreen(onDone = {
                repo.update { it.markTutorialSeen() }
                if (!nav.popBackStack()) {
                    nav.navigate(HomeRoute) { popUpTo<TutorialRoute> { inclusive = true } }
                }
            })
        }
        composable<HomeRoute> {
            LevelScreen(
                onProfiles = { nav.navigate(ProfilesRoute) },
                onCalibration = { nav.navigate(CalibrationRoute) },
                onSettings = { nav.navigate(SettingsRoute) },
                onTutorial = { nav.navigate(TutorialRoute) },
            )
        }
        composable<SettingsRoute> {
            SettingsScreen(onBack = { nav.popBackStack() })
        }
        composable<CalibrationRoute> {
            CalibrationScreen(onBack = { nav.popBackStack() }, onProfiles = { nav.navigate(ProfilesRoute) })
        }
        composable<ProfilesRoute> {
            ProfilesScreen(
                onBack = { nav.popBackStack() },
                onEditVehicle = { nav.navigate(VehicleEditRoute(it)) },
                onEditEquipment = { nav.navigate(EquipmentEditRoute(it)) },
            )
        }
        composable<VehicleEditRoute> { entry ->
            VehicleEditScreen(entry.toRoute<VehicleEditRoute>().id, onDone = { nav.popBackStack() })
        }
        composable<EquipmentEditRoute> { entry ->
            EquipmentEditScreen(entry.toRoute<EquipmentEditRoute>().id, onDone = { nav.popBackStack() })
        }
    }
}
