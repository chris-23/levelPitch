package io.github.cnissler.levelpitch.profiles

import kotlinx.serialization.Serializable

/** What a tester sends back: the setup and everything measured at one pitch. */
@Serializable
data class SessionReport(
    val appVersion: String,
    val device: String,
    val exportedAtMs: Long,
    val vehicle: VehicleProfile,
    val equipment: EquipmentProfile,
    val session: LevelSession,
)

/** JSON report of the active session, or null if there is nothing to report yet. */
fun sessionReport(data: AppData, appVersion: String, device: String, nowMs: Long): String? {
    val session = data.activeSession?.takeIf { it.measurements.isNotEmpty() } ?: return null
    val report = SessionReport(appVersion, device, nowMs, data.activeVehicle!!, data.activeEquipment!!, session)
    return appDataJson.encodeToString(SessionReport.serializer(), report)
}
