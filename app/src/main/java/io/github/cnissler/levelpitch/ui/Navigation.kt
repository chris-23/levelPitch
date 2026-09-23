package io.github.cnissler.levelpitch.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import io.github.cnissler.levelpitch.ui.profiles.EquipmentEditScreen
import io.github.cnissler.levelpitch.ui.profiles.ProfilesScreen
import io.github.cnissler.levelpitch.ui.profiles.VehicleEditScreen
import kotlinx.serialization.Serializable

@Serializable object HomeRoute
@Serializable object CalibrationRoute
@Serializable object ProfilesRoute
@Serializable data class VehicleEditRoute(val id: String? = null)
@Serializable data class EquipmentEditRoute(val id: String? = null)

@Composable
fun LevelPitchNavHost() {
    val nav = rememberNavController()
    NavHost(nav, startDestination = HomeRoute) {
        composable<HomeRoute> {
            // Temporary start screen until the level loop takes over.
            CalibrationScreen(onBack = {}, onProfiles = { nav.navigate(ProfilesRoute) })
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
