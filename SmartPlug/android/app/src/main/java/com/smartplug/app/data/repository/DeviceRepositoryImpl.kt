package com.smartplug.app.data.repository

import com.smartplug.app.data.local.SecureTokenStore
import com.smartplug.app.data.local.db.DeviceDao
import com.smartplug.app.data.local.db.DeviceEntity
import com.smartplug.app.data.remote.ApiClientFactory
import com.smartplug.app.data.remote.ApiResult
import com.smartplug.app.data.remote.map
import com.smartplug.app.data.remote.safeApiCall
import com.smartplug.app.domain.model.ApiFailure
import com.smartplug.app.domain.model.DeviceStatus
import com.smartplug.app.domain.model.ElectricalMeasurement
import com.smartplug.app.domain.model.IntegrationMode
import com.smartplug.app.domain.model.RelayState
import com.smartplug.app.domain.model.SmartPlugDevice
import com.smartplug.app.domain.repository.DeviceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceRepositoryImpl @Inject constructor(
    private val deviceDao: DeviceDao,
    private val tokenStore: SecureTokenStore,
    private val apiClientFactory: ApiClientFactory,
) : DeviceRepository {

    override fun observeDevices(): Flow<List<SmartPlugDevice>> =
        deviceDao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override suspend fun getDevice(deviceId: String): SmartPlugDevice? =
        deviceDao.getById(deviceId)?.toDomain()

    override suspend fun saveDevice(device: SmartPlugDevice) {
        deviceDao.upsert(device.toEntity())
    }

    override suspend fun removeDevice(deviceId: String) {
        deviceDao.deleteById(deviceId)
        tokenStore.clearOwnerToken(deviceId)
    }

    override suspend fun renameDevice(deviceId: String, displayName: String) {
        deviceDao.renameDevice(deviceId, displayName)
    }

    override suspend fun updateLanIp(deviceId: String, lanIp: String) {
        deviceDao.updateLanIp(deviceId, lanIp)
    }

    override suspend fun fetchStatus(device: SmartPlugDevice): ApiResult<DeviceStatus> =
        when (device.integrationMode) {
            IntegrationMode.DIRECT -> fetchDirectStatus(device)
            IntegrationMode.SERVER -> fetchServerStatus(device)
        }

    override suspend fun fetchMeasurement(device: SmartPlugDevice): ApiResult<ElectricalMeasurement> =
        when (device.integrationMode) {
            IntegrationMode.DIRECT -> fetchDirectMeasurement(device)
            IntegrationMode.SERVER -> fetchServerMeasurement(device)
        }

    override suspend fun verifyDeviceIdentity(device: SmartPlugDevice): ApiResult<Boolean> =
        fetchDirectStatus(device).map { it.deviceId == device.deviceId }

    private fun requireOwnerToken(deviceId: String): String? =
        tokenStore.ownerToken(deviceId)?.let { "Bearer $it" }

    private fun missingTokenFailure() = ApiResult.Failure(ApiFailure(0, "missing_owner_token"))
    private fun missingLanIpFailure() = ApiResult.Failure(ApiFailure(0, "missing_lan_ip"))
    private fun missingServerFailure() = ApiResult.Failure(ApiFailure(0, "missing_server_host"))

    private suspend fun fetchDirectStatus(device: SmartPlugDevice): ApiResult<DeviceStatus> {
        val lanIp = device.lanIp ?: return missingLanIpFailure()
        val bearer = requireOwnerToken(device.deviceId) ?: return missingTokenFailure()
        val api = apiClientFactory.deviceApi(ApiClientFactory.lanBaseUrl(lanIp))
        return safeApiCall { api.getStatus(bearer) }.map { dto ->
            DeviceStatus(
                deviceId = dto.deviceId,
                relayState = parseRelayState(dto.relayState),
                relayActuationEnabled = dto.relayActuationEnabled,
                wifiConnected = dto.wifiConnected,
                hasSample = dto.hasSample,
                fresh = dto.fresh,
                sampleAgeMs = dto.sampleAgeMs,
                timerRemainingMs = dto.timer?.remainingMs ?: 0,
                timerArmed = dto.timer?.let { it.active && !it.running } == true,
                scheduleRemainingMs = dto.schedule?.next?.remainingSeconds?.times(1000L) ?: 0,
                scheduleTurnOn = dto.schedule?.next?.state?.let { if (it == "on") true else if (it == "off") false else null },
                energySavedWh = dto.energyPersistence?.takeIf { it.ready && it.savedAvailable }?.savedWh,
                energyNextSaveMs = dto.energyPersistence?.takeIf { it.ready }?.nextSaveSeconds?.times(1000L),
            )
        }
    }

    private suspend fun fetchDirectMeasurement(device: SmartPlugDevice): ApiResult<ElectricalMeasurement> {
        val lanIp = device.lanIp ?: return missingLanIpFailure()
        val bearer = requireOwnerToken(device.deviceId) ?: return missingTokenFailure()
        val api = apiClientFactory.deviceApi(ApiClientFactory.lanBaseUrl(lanIp))
        return safeApiCall { api.getAllParameters(bearer) }.map { dto ->
            ElectricalMeasurement(
                capturedAtMs = dto.capturedAtMs,
                hasSample = dto.hasSample,
                fresh = dto.fresh,
                sampleAgeMs = dto.sampleAgeMs,
                calibrated = dto.calibration == "calibrated",
                voltageV = dto.electrical.voltageV,
                currentA = dto.electrical.currentA,
                activePowerW = dto.electrical.activePowerW,
                apparentPowerVa = dto.electrical.apparentPowerVa,
                powerFactor = dto.electrical.powerFactor,
                energyWh = dto.electrical.energyWh,
            )
        }
    }

    private suspend fun fetchServerStatus(device: SmartPlugDevice): ApiResult<DeviceStatus> {
        val host = device.serverHost ?: return missingServerFailure()
        val bearer = tokenStore.serverApiToken?.let { "Bearer $it" } ?: return missingTokenFailure()
        val api = apiClientFactory.serverApi(ApiClientFactory.hostBaseUrl(host, device.serverPort))
        return safeApiCall { api.getLatest(bearer, device.deviceId) }.map { dto ->
            val ageMs = (System.currentTimeMillis() - dto.capturedAtMs).coerceAtLeast(0)
            DeviceStatus(
                deviceId = dto.deviceId,
                relayState = parseRelayState(dto.relayState),
                relayActuationEnabled = dto.status != "offline",
                wifiConnected = dto.status != "offline",
                hasSample = true,
                fresh = dto.status == "online",
                sampleAgeMs = ageMs,
                timerRemainingMs = when {
                    dto.timerDeadlineUtc > 0 -> (dto.timerDeadlineUtc * 1000L - System.currentTimeMillis()).coerceAtLeast(0)
                    else -> dto.timerDurationSeconds * 1000L
                },
                timerArmed = dto.timerDeadlineUtc == 0L && dto.timerDurationSeconds > 0,
                scheduleRemainingMs = dto.schedule?.next?.remainingSeconds?.times(1000L) ?: 0,
                scheduleTurnOn = dto.schedule?.next?.state?.let { if (it == "on") true else if (it == "off") false else null },
            )
        }
    }

    private suspend fun fetchServerMeasurement(device: SmartPlugDevice): ApiResult<ElectricalMeasurement> {
        val host = device.serverHost ?: return missingServerFailure()
        val bearer = tokenStore.serverApiToken?.let { "Bearer $it" } ?: return missingTokenFailure()
        val api = apiClientFactory.serverApi(ApiClientFactory.hostBaseUrl(host, device.serverPort))
        return safeApiCall { api.getLatest(bearer, device.deviceId) }.map { dto ->
            val ageMs = (System.currentTimeMillis() - dto.capturedAtMs).coerceAtLeast(0)
            ElectricalMeasurement(
                capturedAtMs = dto.capturedAtMs,
                hasSample = true,
                fresh = dto.status == "online",
                sampleAgeMs = ageMs,
                calibrated = dto.calibrated,
                voltageV = dto.voltageV,
                currentA = dto.currentA,
                activePowerW = dto.activePowerW,
                apparentPowerVa = dto.apparentPowerVa,
                powerFactor = dto.powerFactor,
                energyWh = dto.energyWh,
            )
        }
    }

    companion object {
        fun parseRelayState(wireValue: String): RelayState = when (wireValue) {
            "on" -> RelayState.ON
            "off" -> RelayState.OFF
            else -> RelayState.UNKNOWN
        }
    }
}

private fun DeviceEntity.toDomain() = SmartPlugDevice(
    deviceId = deviceId,
    staMac = staMac,
    displayName = displayName,
    lanIp = lanIp,
    integrationMode = if (integrationMode == "server") IntegrationMode.SERVER else IntegrationMode.DIRECT,
    serverId = serverId,
    serverHost = serverHost,
    serverPort = serverPort,
    apUnitId = apUnitId,
)

private fun SmartPlugDevice.toEntity() = DeviceEntity(
    deviceId = deviceId,
    staMac = staMac,
    displayName = displayName,
    lanIp = lanIp,
    integrationMode = if (integrationMode == IntegrationMode.SERVER) "server" else "direct",
    serverId = serverId,
    serverHost = serverHost,
    serverPort = serverPort,
    apUnitId = apUnitId,
)
