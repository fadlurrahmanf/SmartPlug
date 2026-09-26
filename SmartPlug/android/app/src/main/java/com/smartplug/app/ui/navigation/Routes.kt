package com.smartplug.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Routes(val route: String) {
    data object Home : Routes("home")
    data object Devices : Routes("devices")
    data object AddSmartPlug : Routes("add_smartplug")
    data object Settings : Routes("settings")

    data object DeviceDetail : Routes("device/{deviceId}") {
        fun createRoute(deviceId: String) = "device/$deviceId"
        const val ARG_DEVICE_ID = "deviceId"
    }

    data object EnergyHistory : Routes("device/{deviceId}/history") {
        fun createRoute(deviceId: String) = "device/$deviceId/history"
        const val ARG_DEVICE_ID = "deviceId"
    }

    data object Schedule : Routes("device/{deviceId}/schedule") {
        fun createRoute(deviceId: String) = "device/$deviceId/schedule"
        const val ARG_DEVICE_ID = "deviceId"
    }
}

/** Top-level destinations shown in the nav rail / bottom bar / drawer. */
data class TopLevelDestination(
    val route: String,
    val labelIndonesian: String,
    val labelEnglish: String,
    val icon: ImageVector,
)

val topLevelDestinations = listOf(
    TopLevelDestination(Routes.Home.route, "Beranda", "Home", Icons.Filled.Home),
    TopLevelDestination(Routes.Devices.route, "Perangkat", "Devices", Icons.Filled.Power),
    TopLevelDestination(Routes.Settings.route, "Pengaturan", "Settings", Icons.Filled.Settings),
)
