package com.smartplug.app.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/** DTOs for SmartPlug's operational REST API, `firmware/LOCAL-API.md` + design.md "REST operasional". */

@JsonClass(generateAdapter = true)
data class ApiErrorEnvelopeDto(
    @Json(name = "api_version") val apiVersion: String? = null,
    @Json(name = "error") val error: ApiErrorBodyDto? = null,
)

@JsonClass(generateAdapter = true)
data class ApiErrorBodyDto(
    @Json(name = "code") val code: String,
    @Json(name = "message") val message: String? = null,
)

@JsonClass(generateAdapter = true)
data class DeviceStatusDto(
    @Json(name = "api_version") val apiVersion: String,
    @Json(name = "device_id") val deviceId: String,
    @Json(name = "relay_state") val relayState: String,
    @Json(name = "relay_actuation") val relayActuationEnabled: Boolean = true,
    @Json(name = "wifi_connected") val wifiConnected: Boolean = true,
    @Json(name = "has_sample") val hasSample: Boolean = false,
    @Json(name = "fresh") val fresh: Boolean = false,
    @Json(name = "sample_age_ms") val sampleAgeMs: Long = 0,
    @Json(name = "timer") val timer: DeviceTimerDto? = null,
    @Json(name = "schedule") val schedule: DeviceScheduleDto? = null,
    @Json(name = "energy_persistence") val energyPersistence: EnergyPersistenceDto? = null,
)

@JsonClass(generateAdapter = true)
data class EnergyPersistenceDto(
    @Json(name = "ready") val ready: Boolean = false,
    @Json(name = "saved_available") val savedAvailable: Boolean = false,
    @Json(name = "saved_wh") val savedWh: Double = 0.0,
    @Json(name = "next_save_seconds") val nextSaveSeconds: Long? = null,
)

@JsonClass(generateAdapter = true)
data class DeviceTimerDto(
    @Json(name = "active") val active: Boolean = false,
    @Json(name = "running") val running: Boolean = false,
    @Json(name = "remaining_ms") val remainingMs: Long = 0,
)

@JsonClass(generateAdapter = true)
data class DeviceScheduleEntryDto(
    @Json(name = "hour") val hour: Int,
    @Json(name = "minute") val minute: Int,
    @Json(name = "state") val state: String,
    @Json(name = "event") val event: String = "",
)

@JsonClass(generateAdapter = true)
data class DeviceScheduleClockDto(
    @Json(name = "synchronized") val synchronized: Boolean = false,
    @Json(name = "utc_ms") val utcMs: Long = 0,
    @Json(name = "timezone_offset_minutes") val timezoneOffsetMinutes: Int = 0,
)

@JsonClass(generateAdapter = true)
data class DeviceScheduleNextDto(
    @Json(name = "remaining_seconds") val remainingSeconds: Long = 0,
    @Json(name = "state") val state: String = "",
)

@JsonClass(generateAdapter = true)
data class DeviceScheduleDto(
    @Json(name = "enabled") val enabled: Boolean = false,
    @Json(name = "clock") val clock: DeviceScheduleClockDto = DeviceScheduleClockDto(),
    @Json(name = "next") val next: DeviceScheduleNextDto = DeviceScheduleNextDto(),
    @Json(name = "entries") val entries: List<DeviceScheduleEntryDto> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class ElectricalDto(
    @Json(name = "voltage_v") val voltageV: Double,
    @Json(name = "current_a") val currentA: Double,
    @Json(name = "active_power_w") val activePowerW: Double,
    @Json(name = "apparent_power_va") val apparentPowerVa: Double,
    @Json(name = "power_factor") val powerFactor: Double,
    @Json(name = "energy_wh") val energyWh: Double,
)

@JsonClass(generateAdapter = true)
data class AllParametersResponseDto(
    @Json(name = "captured_at_ms") val capturedAtMs: Long,
    @Json(name = "has_sample") val hasSample: Boolean,
    @Json(name = "fresh") val fresh: Boolean = false,
    @Json(name = "sample_age_ms") val sampleAgeMs: Long = 0,
    @Json(name = "calibration") val calibration: String,
    @Json(name = "electrical") val electrical: ElectricalDto,
)

@JsonClass(generateAdapter = true)
data class RelayCommandRequestDto(
    @Json(name = "state") val state: String,
)

@JsonClass(generateAdapter = true)
data class RelayCommandResponseDto(
    @Json(name = "api_version") val apiVersion: String,
    @Json(name = "command_id") val commandId: String,
    @Json(name = "state") val state: String,
    @Json(name = "status") val status: String,
)
