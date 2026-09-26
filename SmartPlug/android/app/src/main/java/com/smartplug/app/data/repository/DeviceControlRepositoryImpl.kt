package com.smartplug.app.data.repository

import com.smartplug.app.data.local.SecureTokenStore
import com.smartplug.app.data.remote.ApiClientFactory
import com.smartplug.app.data.remote.ApiResult
import com.smartplug.app.data.remote.safeApiCall
import com.smartplug.app.data.remote.map
import com.smartplug.app.domain.model.ApiFailure
import com.smartplug.app.domain.model.IntegrationMode
import com.smartplug.app.domain.model.DeviceSchedule
import com.smartplug.app.domain.model.DailyScheduleEntry
import com.smartplug.app.domain.model.SmartPlugDevice
import com.smartplug.app.domain.repository.DeviceControlRepository
import javax.inject.Inject
import javax.inject.Singleton

/** Routes irreversible control operations through the same owner credentials as relay control. */
@Singleton
class DeviceControlRepositoryImpl @Inject constructor(
    private val tokenStore: SecureTokenStore,
    private val apiClientFactory: ApiClientFactory,
) : DeviceControlRepository {

    override suspend fun resetEnergy(device: SmartPlugDevice): ApiResult<Unit> = when (device.integrationMode) {
        IntegrationMode.DIRECT -> direct(device) { api, bearer -> api.resetEnergy(bearer, energyConfirmation()) }
        IntegrationMode.SERVER -> server(device) { api, bearer -> api.resetEnergy(bearer, device.deviceId, energyConfirmation()) }
    }

    override suspend fun factoryReset(device: SmartPlugDevice): ApiResult<Unit> = when (device.integrationMode) {
        IntegrationMode.DIRECT -> direct(device) { api, bearer -> api.factoryReset(bearer, factoryConfirmation()) }
        IntegrationMode.SERVER -> server(device) { api, bearer -> api.factoryReset(bearer, device.deviceId, factoryConfirmation()) }
    }

    override suspend fun applyTimer(
        device: SmartPlugDevice, days: Int, hours: Int, minutes: Int, seconds: Int,
    ): ApiResult<Unit> {
        if (days < 0 || hours !in 0..23 || minutes !in 0..59 || seconds !in 0..59 ||
            days == 0 && hours == 0 && minutes == 0 && seconds == 0) {
            return ApiResult.Failure(ApiFailure(400, "invalid_timer_duration"))
        }
        return when (device.integrationMode) {
            IntegrationMode.DIRECT -> direct(device) { api, bearer ->
                api.setTimer(bearer, timerForm("apply", days, hours, minutes, seconds))
            }
            IntegrationMode.SERVER -> server(device) { api, bearer ->
                api.setTimer(bearer, device.deviceId, timerBody("apply", days, hours, minutes, seconds))
            }
        }
    }

    override suspend fun resetTimer(device: SmartPlugDevice): ApiResult<Unit> = when (device.integrationMode) {
        IntegrationMode.DIRECT -> direct(device) { api, bearer -> api.setTimer(bearer, mapOf("action" to "reset")) }
        IntegrationMode.SERVER -> server(device) { api, bearer -> api.setTimer(bearer, device.deviceId, mapOf("action" to "reset")) }
    }

    override suspend fun getSchedule(device: SmartPlugDevice): ApiResult<DeviceSchedule> = when (device.integrationMode) {
        IntegrationMode.DIRECT -> scheduleDirect(device) { api, bearer -> api.getSchedule(bearer) }
        IntegrationMode.SERVER -> scheduleServer(device) { api, bearer -> api.getSchedule(bearer, device.deviceId) }
    }

    override suspend fun setScheduleEnabled(
        device: SmartPlugDevice, enabled: Boolean, timezoneOffsetMinutes: Int,
    ): ApiResult<DeviceSchedule> = when (device.integrationMode) {
        IntegrationMode.DIRECT -> scheduleDirect(device) { api, bearer -> api.setSchedule(bearer, scheduleForm("set_enabled", timezoneOffsetMinutes) + mapOf("enabled" to enabled.toString())) }
        IntegrationMode.SERVER -> scheduleServer(device) { api, bearer -> api.setSchedule(bearer, device.deviceId, scheduleBody("set_enabled", timezoneOffsetMinutes) + mapOf("enabled" to enabled)) }
    }

    override suspend fun addSchedule(
        device: SmartPlugDevice, hour: Int, minute: Int, turnOn: Boolean, event: String, timezoneOffsetMinutes: Int,
    ): ApiResult<DeviceSchedule> {
        if (hour !in 0..23 || minute !in 0..59) return ApiResult.Failure(ApiFailure(400, "invalid_schedule_entry"))
        val normalizedEvent = event.trim()
        return when (device.integrationMode) {
            IntegrationMode.DIRECT -> scheduleDirect(device) { api, bearer ->
                api.setSchedule(bearer, scheduleForm("add", timezoneOffsetMinutes) + mapOf("hour" to hour.toString(), "minute" to minute.toString(), "state" to if (turnOn) "on" else "off", "event" to normalizedEvent))
            }
            IntegrationMode.SERVER -> scheduleServer(device) { api, bearer ->
                api.setSchedule(bearer, device.deviceId, scheduleBody("add", timezoneOffsetMinutes) + mapOf("hour" to hour, "minute" to minute, "state" to if (turnOn) "on" else "off", "event" to normalizedEvent))
            }
        }
    }

    override suspend fun deleteSchedule(device: SmartPlugDevice, index: Int): ApiResult<DeviceSchedule> =
        when (device.integrationMode) {
            IntegrationMode.DIRECT -> scheduleDirect(device) { api, bearer -> api.setSchedule(bearer, mapOf("action" to "delete", "index" to index.toString())) }
            IntegrationMode.SERVER -> scheduleServer(device) { api, bearer -> api.setSchedule(bearer, device.deviceId, mapOf("action" to "delete", "index" to index)) }
        }

    override suspend fun moveSchedule(device: SmartPlugDevice, from: Int, to: Int): ApiResult<DeviceSchedule> =
        when (device.integrationMode) {
            IntegrationMode.DIRECT -> scheduleDirect(device) { api, bearer -> api.setSchedule(bearer, mapOf("action" to "move", "from" to from.toString(), "to" to to.toString())) }
            IntegrationMode.SERVER -> scheduleServer(device) { api, bearer -> api.setSchedule(bearer, device.deviceId, mapOf("action" to "move", "from" to from, "to" to to)) }
        }

    private suspend fun direct(
        device: SmartPlugDevice,
        request: suspend (com.smartplug.app.data.remote.DeviceApi, String) -> retrofit2.Response<Unit>,
    ): ApiResult<Unit> {
        val ip = device.lanIp ?: return ApiResult.Failure(ApiFailure(0, "missing_lan_ip"))
        val bearer = tokenStore.ownerToken(device.deviceId)?.let { "Bearer $it" }
            ?: return ApiResult.Failure(ApiFailure(0, "missing_owner_token"))
        return safeApiCall { request(apiClientFactory.deviceApi(ApiClientFactory.lanBaseUrl(ip)), bearer) }
    }

    private suspend fun server(
        device: SmartPlugDevice,
        request: suspend (com.smartplug.app.data.remote.ServerApi, String) -> retrofit2.Response<Unit>,
    ): ApiResult<Unit> {
        val host = device.serverHost ?: return ApiResult.Failure(ApiFailure(0, "missing_server_host"))
        val bearer = tokenStore.serverApiToken?.let { "Bearer $it" }
            ?: return ApiResult.Failure(ApiFailure(0, "missing_server_token"))
        return safeApiCall { request(apiClientFactory.serverApi(ApiClientFactory.hostBaseUrl(host, device.serverPort)), bearer) }
    }

    private suspend fun scheduleDirect(
        device: SmartPlugDevice,
        request: suspend (com.smartplug.app.data.remote.DeviceApi, String) -> retrofit2.Response<com.smartplug.app.data.remote.dto.DeviceScheduleDto>,
    ): ApiResult<DeviceSchedule> {
        val ip = device.lanIp ?: return ApiResult.Failure(ApiFailure(0, "missing_lan_ip"))
        val bearer = tokenStore.ownerToken(device.deviceId)?.let { "Bearer $it" }
            ?: return ApiResult.Failure(ApiFailure(0, "missing_owner_token"))
        return safeApiCall { request(apiClientFactory.deviceApi(ApiClientFactory.lanBaseUrl(ip)), bearer) }.map { dto ->
            dto.toSchedule()
        }
    }

    private suspend fun scheduleServer(
        device: SmartPlugDevice,
        request: suspend (com.smartplug.app.data.remote.ServerApi, String) -> retrofit2.Response<com.smartplug.app.data.remote.dto.DeviceScheduleDto>,
    ): ApiResult<DeviceSchedule> {
        val host = device.serverHost ?: return ApiResult.Failure(ApiFailure(0, "missing_server_host"))
        val bearer = tokenStore.serverApiToken?.let { "Bearer $it" }
            ?: return ApiResult.Failure(ApiFailure(0, "missing_server_token"))
        return safeApiCall { request(apiClientFactory.serverApi(ApiClientFactory.hostBaseUrl(host, device.serverPort)), bearer) }.map { it.toSchedule() }
    }

    private fun energyConfirmation() = mapOf(
        "confirm_1" to "RESET_ENERGY", "confirm_2" to "RESET_ENERGY", "confirm_3" to "RESET_ENERGY",
    )
    private fun factoryConfirmation() = mapOf(
        "confirm_1" to "FACTORY_RESET", "confirm_2" to "FACTORY_RESET", "confirm_3" to "FACTORY_RESET",
    )
    private fun timerForm(action: String, days: Int = 0, hours: Int = 0, minutes: Int = 0, seconds: Int = 0) = mapOf(
        "action" to action, "days" to days.toString(), "hours" to hours.toString(),
        "minutes" to minutes.toString(), "seconds" to seconds.toString(),
    )
    private fun timerBody(action: String, days: Int = 0, hours: Int = 0, minutes: Int = 0, seconds: Int = 0): Map<String, Any> = mapOf(
        "action" to action, "days" to days, "hours" to hours, "minutes" to minutes, "seconds" to seconds,
    )
    private fun scheduleForm(action: String, timezoneOffsetMinutes: Int): Map<String, String> = mapOf(
        "action" to action,
        "timezone_offset_minutes" to timezoneOffsetMinutes.coerceIn(-720, 840).toString(),
    )
    private fun scheduleBody(action: String, timezoneOffsetMinutes: Int): Map<String, Any> = mapOf(
        "action" to action,
        "timezone_offset_minutes" to timezoneOffsetMinutes.coerceIn(-720, 840),
    )
}

private fun com.smartplug.app.data.remote.dto.DeviceScheduleDto.toSchedule() = DeviceSchedule(
    enabled = enabled,
    clockSynchronized = clock.synchronized,
    timezoneOffsetMinutes = clock.timezoneOffsetMinutes,
    nextRemainingSeconds = next.remainingSeconds,
    nextTurnOn = when (next.state) { "on" -> true; "off" -> false; else -> null },
    entries = entries.map { DailyScheduleEntry(it.hour, it.minute, it.state == "on", it.event) },
)
