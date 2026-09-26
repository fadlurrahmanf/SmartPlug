package com.smartplug.app.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/** DTOs for ServerSmartPlug's application REST API, design.md "REST API aplikasi ke server". */

@JsonClass(generateAdapter = true)
data class ServerDeviceDto(
    @Json(name = "device_id") val deviceId: String,
    @Json(name = "display_name") val displayName: String? = null,
    @Json(name = "status") val status: String,
)

@JsonClass(generateAdapter = true)
data class ServerLatestDto(
    @Json(name = "device_id") val deviceId: String,
    @Json(name = "captured_at_ms") val capturedAtMs: Long,
    @Json(name = "status") val status: String,
    @Json(name = "relay_state") val relayState: String,
    @Json(name = "calibrated") val calibrated: Boolean = false,
    @Json(name = "voltage_v") val voltageV: Double = 0.0,
    @Json(name = "current_a") val currentA: Double = 0.0,
    @Json(name = "active_power_w") val activePowerW: Double = 0.0,
    @Json(name = "apparent_power_va") val apparentPowerVa: Double = 0.0,
    @Json(name = "power_factor") val powerFactor: Double = 0.0,
    @Json(name = "energy_wh") val energyWh: Double = 0.0,
    @Json(name = "timer_deadline_utc") val timerDeadlineUtc: Long = 0,
    @Json(name = "timer_duration_seconds") val timerDurationSeconds: Long = 0,
    @Json(name = "schedule") val schedule: DeviceScheduleDto? = null,
)

@JsonClass(generateAdapter = true)
data class ServerEnergyDto(
    @Json(name = "device_id") val deviceId: String,
    @Json(name = "energy_wh") val energyWh: Double,
    @Json(name = "source") val source: String,
    @Json(name = "recorded_at_ms") val recordedAtMs: Long,
)

@JsonClass(generateAdapter = true)
data class ServerHistoryPointDto(
    @Json(name = "timestamp_utc_ms") val timestampUtcMs: Long,
    @Json(name = "voltage_v") val voltageV: Double,
    @Json(name = "current_a") val currentA: Double,
    @Json(name = "active_power_w") val activePowerW: Double,
    @Json(name = "apparent_power_va") val apparentPowerVa: Double,
    @Json(name = "power_factor") val powerFactor: Double,
    @Json(name = "energy_wh") val energyWh: Double,
)

@JsonClass(generateAdapter = true)
data class ServerHistoryResponseDto(
    @Json(name = "device_id") val deviceId: String,
    @Json(name = "resolution") val resolution: String,
    @Json(name = "points") val points: List<ServerHistoryPointDto>,
)

@JsonClass(generateAdapter = true)
data class ServerRelayResponseDto(
    @Json(name = "command_id") val commandId: String,
    @Json(name = "status") val status: String,
    @Json(name = "state") val state: String,
)

@JsonClass(generateAdapter = true)
data class ServerCommandStatusDto(
    @Json(name = "command_id") val commandId: String,
    @Json(name = "status") val status: String,
    @Json(name = "state") val state: String? = null,
)
