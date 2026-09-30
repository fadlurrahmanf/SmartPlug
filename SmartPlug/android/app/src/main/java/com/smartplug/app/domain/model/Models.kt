package com.smartplug.app.domain.model

/**
 * Converts a Wi-Fi RSSI reading (dBm, typically -100..-50) into a 0-100% signal strength for
 * display, using the same linear scale Android's own Wi-Fi picker is based on: -50 dBm or
 * stronger reads as 100%, -100 dBm or weaker reads as 0%.
 */
fun rssiToSignalPercent(rssi: Int): Int = when {
    rssi >= -50 -> 100
    rssi <= -100 -> 0
    else -> 2 * (rssi + 100)
}

/** How the app is talking to a given SmartPlug. Mirrors design.md `connection_profile.type`. */
enum class IntegrationMode {
    /** App -> SmartPlug REST directly, no ServerSmartPlug involved. */
    DIRECT,

    /** App -> ServerSmartPlug REST; SmartPlug <-> ServerSmartPlug over MQTT. */
    SERVER,
}

/** A SmartPlug the user has finished onboarding. Persisted locally (Room). */
data class SmartPlugDevice(
    val deviceId: String,
    val staMac: String,
    val displayName: String,
    val lanIp: String?,
    val integrationMode: IntegrationMode,
    /** Only set when [integrationMode] is [IntegrationMode.SERVER]. */
    val serverId: String? = null,
    val serverHost: String? = null,
    val serverPort: Int = 80,
    /** The pairing AP's unit id (SSID "SP-<unitId>"), so a re-scan can hide already-paired units. */
    val apUnitId: String? = null,
)

/** One AP advertised by an unprovisioned unit, as seen by a Wi-Fi scan (SSID `SP-<unit_id>`). */
data class DiscoveredSmartPlugAp(
    val ssid: String,
    val unitId: String,
    val rssi: Int,
) {
    val apPassword: String get() = "setup-$unitId"
}

/** A ServerSmartPlug discovered on the home Wi-Fi during onboarding (`_srvrplug._tcp`). */
data class DiscoveredServer(
    val serverId: String,
    val host: String,
    val port: Int,
)

/** A ServerSmartPlug registered in this app.  Its secret API token stays only in encrypted storage. */
data class RegisteredServer(
    val serverId: String,
    val displayName: String,
    val host: String,
    val mqttPort: Int = 1883,
    val mqttUsername: String = "SmartPlug",
    val mqttPassword: String = "deviotsolution",
)

/** A Wi-Fi network reported by SmartPlug's own `/api/v1/pair/scan-wifi`. */
data class HomeWifiNetwork(
    val ssid: String,
    val rssi: Int,
    val security: String,
)

/** Result of `GET /api/v1/pair/info`. */
data class PairInfo(
    val apiVersion: String,
    val product: String,
    val protocol: String,
    val deviceId: String,
    val staMac: String,
    val state: String,
    val pairingToken: String,
    val tokenExpiresInS: Int,
)

/** Server profile the app forwards to SmartPlug inside `/api/v1/pair/configure` when the user
 * opts into ServerSmartPlug (design.md "Profil MQTT yang dikirim aplikasi ke SmartPlug"). */
data class ServerConnectionProfile(
    val serverId: String,
    val brokerHost: String,
    val brokerPort: Int,
    val mqttUsername: String,
    val mqttPassword: String,
    val baseTopic: String,
)

/** State machine for one onboarding attempt, mirrors design.md "Keadaan perangkat". */
enum class PairingState {
    UNPROVISIONED,
    PAIRING,
    CONNECTING,
    CONNECTED,
    FAILED,
}

/** `reason` values from `GET /api/v1/pair/status` on failure. */
enum class PairingFailureReason(val wireValue: String) {
    PAIRING_CLOSED("pairing_closed"),
    INVALID_PAIRING_TOKEN("invalid_pairing_token"),
    WIFI_NOT_FOUND("wifi_not_found"),
    WIFI_AUTHENTICATION_FAILED("wifi_authentication_failed"),
    WIFI_CONNECTION_TIMEOUT("wifi_connection_timeout"),
    SERVER_PROFILE_INVALID("server_profile_invalid"),
    BROKER_CONNECTION_FAILED("broker_connection_failed"),
    UNKNOWN("unknown");

    companion object {
        fun from(wireValue: String?): PairingFailureReason =
            entries.firstOrNull { it.wireValue == wireValue } ?: UNKNOWN
    }
}

data class PairingStatus(
    val state: PairingState,
    val deviceId: String? = null,
    val staMac: String? = null,
    val lanIp: String? = null,
    val ownerToken: String? = null,
    val ssid: String? = null,
    val failureReason: PairingFailureReason? = null,
)

/** Logical relay state as reported by the device/server, never physical contact feedback. */
enum class RelayState { ON, OFF, UNKNOWN }

data class ElectricalMeasurement(
    val capturedAtMs: Long,
    val hasSample: Boolean,
    val fresh: Boolean,
    val sampleAgeMs: Long,
    val calibrated: Boolean,
    val voltageV: Double,
    val currentA: Double,
    val activePowerW: Double,
    val apparentPowerVa: Double,
    val powerFactor: Double,
    val energyWh: Double,
)

data class DeviceStatus(
    val deviceId: String,
    val relayState: RelayState,
    val relayActuationEnabled: Boolean,
    val wifiConnected: Boolean,
    val hasSample: Boolean,
    val fresh: Boolean,
    val sampleAgeMs: Long,
    val timerRemainingMs: Long = 0,
    val timerArmed: Boolean = false,
    val scheduleRemainingMs: Long = 0,
    val scheduleTurnOn: Boolean? = null,
    /** Direct-mode LittleFS checkpoint status; null means this source does not expose it. */
    val energySavedWh: Double? = null,
    val energyNextSaveMs: Long? = null,
)

data class DailyScheduleEntry(
    val hour: Int,
    val minute: Int,
    val turnOn: Boolean,
    val event: String = "",
)
data class DeviceSchedule(
    val enabled: Boolean,
    val clockSynchronized: Boolean,
    val timezoneOffsetMinutes: Int,
    val nextRemainingSeconds: Long,
    val nextTurnOn: Boolean?,
    val entries: List<DailyScheduleEntry>,
)

enum class RelayCommandStatus { QUEUED, COMPLETED, REJECTED, TIMEOUT }

data class RelayCommandResult(
    val commandId: String,
    val status: RelayCommandStatus,
    val state: RelayState,
)

data class EnergyHistoryPoint(
    val timestampUtcMs: Long,
    val voltageV: Double,
    val currentA: Double,
    val activePowerW: Double,
    val apparentPowerVa: Double,
    val powerFactor: Double,
    val energyWh: Double,
)

enum class HistoryResolution(val wireValue: String) {
    ONE_MINUTE("1m"),
    FIVE_MINUTES("5m"),
    THIRTY_MINUTES("30m"),
    ONE_HOUR("1h"),
    ONE_DAY("1d"),
}

/** Uniform error shape the UI renders; wraps both DIRECT (`error.code`) and SERVER API failures. */
data class ApiFailure(
    val httpCode: Int,
    val errorCode: String,
    val message: String? = null,
)
