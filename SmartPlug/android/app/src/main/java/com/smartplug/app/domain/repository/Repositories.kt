package com.smartplug.app.domain.repository

import com.smartplug.app.data.remote.ApiResult
import com.smartplug.app.domain.model.DeviceStatus
import com.smartplug.app.domain.model.DeviceSchedule
import com.smartplug.app.domain.model.DiscoveredServer
import com.smartplug.app.domain.model.DiscoveredSmartPlugAp
import com.smartplug.app.domain.model.ElectricalMeasurement
import com.smartplug.app.domain.model.EnergyHistoryPoint
import com.smartplug.app.domain.model.HistoryResolution
import com.smartplug.app.domain.model.HomeWifiNetwork
import com.smartplug.app.domain.model.PairInfo
import com.smartplug.app.domain.model.PairingStatus
import com.smartplug.app.domain.model.RelayCommandResult
import com.smartplug.app.domain.model.RelayState
import com.smartplug.app.domain.model.ServerConnectionProfile
import com.smartplug.app.domain.model.SmartPlugDevice
import kotlinx.coroutines.flow.Flow

/** Wraps Android's Wi-Fi APIs for the onboarding flow only (design.md "Alur sistem"). */
interface WifiOnboardingRepository {
    /** True if the runtime permission needed to read scan results/connect is already granted. */
    fun hasRequiredPermission(): Boolean

    /** On Android 12 and below, Wi-Fi scan results are silently empty — not an error, not a
     * permission failure, just always empty — unless the phone's system Location toggle is also
     * on, regardless of the app's own permission grant. Android 13+ with NEARBY_WIFI_DEVICES
     * doesn't have this requirement. */
    fun isLocationServiceRequiredAndDisabled(): Boolean

    /** Scans nearby Wi-Fi and returns only APs advertised as `SP-<unit_id>`. */
    suspend fun scanForSmartPlugAps(): Result<List<DiscoveredSmartPlugAp>>

    /** Requests a bound connection to the SmartPlug AP via `WifiNetworkSpecifier` and routes all
     * pairing HTTP calls over it until [releaseApBinding] is called. */
    suspend fun connectToAp(ssid: String, password: String): Result<Unit>

    /** Releases the AP binding so the phone falls back to its normal (home) Wi-Fi network. */
    fun releaseApBinding()

    /** Suspends until the phone is back on a non-SmartPlug Wi-Fi network, or times out. */
    suspend fun awaitHomeWifiRestored(timeoutMs: Long): Boolean
}

/** NSD/mDNS lookups, design.md "Standar nama discovery" + "Jika IP berubah". */
interface DiscoveryRepository {
    suspend fun discoverServer(timeoutMs: Long): DiscoveredServer?
    suspend fun resolveDeviceLanIp(deviceId: String, timeoutMs: Long): String?
}

/** SmartPlug AP-only pairing calls, design.md "Pairing API". */
interface PairingRepository {
    suspend fun fetchPairInfo(): ApiResult<PairInfo>
    suspend fun scanHomeWifi(pairingToken: String): ApiResult<List<HomeWifiNetwork>>
    suspend fun configureDirect(pairingToken: String, ssid: String, password: String): ApiResult<String>
    suspend fun configureWithServer(
        pairingToken: String,
        ssid: String,
        password: String,
        serverProfile: ServerConnectionProfile,
    ): ApiResult<String>

    /** Emits one [PairingStatus] per second until `connected`/`failed`, per design.md. */
    fun pollStatus(pairingToken: String, configurationId: String): Flow<ApiResult<PairingStatus>>
}

/** Local device registry + operational status/measurement reads, routed by [SmartPlugDevice.integrationMode]. */
interface DeviceRepository {
    fun observeDevices(): Flow<List<SmartPlugDevice>>
    suspend fun getDevice(deviceId: String): SmartPlugDevice?
    suspend fun saveDevice(device: SmartPlugDevice)
    suspend fun removeDevice(deviceId: String)
    suspend fun renameDevice(deviceId: String, displayName: String)
    suspend fun updateLanIp(deviceId: String, lanIp: String)

    suspend fun fetchStatus(device: SmartPlugDevice): ApiResult<DeviceStatus>
    suspend fun fetchMeasurement(device: SmartPlugDevice): ApiResult<ElectricalMeasurement>

    /** Verifies the LAN `device_id` matches after onboarding hands control back to home Wi-Fi. */
    suspend fun verifyDeviceIdentity(device: SmartPlugDevice): ApiResult<Boolean>
}

/** Relay control, design.md "Status relay": command acceptance != physical settle. */
interface RelayRepository {
    suspend fun setRelay(device: SmartPlugDevice, targetOn: Boolean): ApiResult<RelayCommandResult>

    /** Polls until the device/server reports the settled [RelayState], or times out. */
    suspend fun awaitSettledState(
        device: SmartPlugDevice,
        commandId: String,
        targetState: RelayState,
    ): ApiResult<RelayCommandResult>
}

/** Safety-sensitive owner actions and countdown configuration. */
interface DeviceControlRepository {
    suspend fun resetEnergy(device: SmartPlugDevice): ApiResult<Unit>
    suspend fun factoryReset(device: SmartPlugDevice): ApiResult<Unit>
    suspend fun applyTimer(device: SmartPlugDevice, days: Int, hours: Int, minutes: Int, seconds: Int): ApiResult<Unit>
    suspend fun resetTimer(device: SmartPlugDevice): ApiResult<Unit>
    suspend fun getSchedule(device: SmartPlugDevice): ApiResult<DeviceSchedule>
    suspend fun setScheduleEnabled(device: SmartPlugDevice, enabled: Boolean, timezoneOffsetMinutes: Int): ApiResult<DeviceSchedule>
    suspend fun addSchedule(device: SmartPlugDevice, hour: Int, minute: Int, turnOn: Boolean, event: String, timezoneOffsetMinutes: Int): ApiResult<DeviceSchedule>
    suspend fun deleteSchedule(device: SmartPlugDevice, index: Int): ApiResult<DeviceSchedule>
    suspend fun moveSchedule(device: SmartPlugDevice, from: Int, to: Int): ApiResult<DeviceSchedule>
}

/** Server-mode energy history, design.md "REST API aplikasi ke server" + "Retensi riwayat". */
interface HistoryRepository {
    suspend fun fetchHistory(
        device: SmartPlugDevice,
        fromUtcMs: Long,
        toUtcMs: Long,
        resolution: HistoryResolution,
    ): ApiResult<List<EnergyHistoryPoint>>
}
