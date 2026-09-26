package com.smartplug.app.ui.screens.devicedetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.smartplug.app.data.remote.ApiResult
import com.smartplug.app.data.local.db.HistoryDao
import com.smartplug.app.data.local.db.HistoryPointEntity
import com.smartplug.app.data.local.db.LoadSignatureDao
import com.smartplug.app.data.local.db.LoadSignatureEntity
import com.smartplug.app.domain.model.ApiFailure
import com.smartplug.app.domain.model.DeviceStatus
import com.smartplug.app.domain.model.ElectricalMeasurement
import com.smartplug.app.domain.model.RelayCommandStatus
import com.smartplug.app.domain.model.RelayState
import com.smartplug.app.domain.model.SmartPlugDevice
import com.smartplug.app.domain.repository.DeviceRepository
import com.smartplug.app.domain.repository.DeviceControlRepository
import com.smartplug.app.domain.repository.RelayRepository
import com.smartplug.app.util.safeLaunch
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.TimeUnit
import javax.inject.Inject

enum class RelayUiState { IDLE, SENDING, WAITING_SETTLE, ERROR }

data class DeviceDetailUiState(
    val device: SmartPlugDevice? = null,
    val status: DeviceStatus? = null,
    val statusReceivedAtMs: Long = 0L,
    val measurement: ElectricalMeasurement? = null,
    val lastError: String? = null,
    val relayUiState: RelayUiState = RelayUiState.IDLE,
    val isRemoved: Boolean = false,
    val liveModeRequested: Boolean = false,
    val livePoints: List<LiveMeasurementPoint> = emptyList(),
    val energyPerMinuteWh: Double? = null,
    val ratesPerMinute: MeasurementRates? = null,
    val canSaveLoadSignature: Boolean = false,
    val detectedLoadName: String? = null,
    val vampireEnergySuspected: Boolean = false,
)

data class LiveMeasurementPoint(val timestampMs: Long, val measurement: ElectricalMeasurement)
data class MeasurementRates(
    val voltageVariationPercent: Double,
    val activePowerW: Double,
    val powerFactorVariationPercent: Double,
    /** Instantaneous conversion from active power, so this responds before the next CF pulse. */
    val energyKwhPerMinute: Double,
)

@HiltViewModel
class DeviceDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val deviceRepository: DeviceRepository,
    private val relayRepository: RelayRepository,
    private val deviceControlRepository: DeviceControlRepository,
    private val historyDao: HistoryDao,
    private val loadSignatureDao: LoadSignatureDao,
) : ViewModel() {

    private val deviceId: String = checkNotNull(savedStateHandle["deviceId"])
    private var lastStoredMinute: Long = Long.MIN_VALUE

    private val _uiState = MutableStateFlow(DeviceDetailUiState())
    val uiState: StateFlow<DeviceDetailUiState> = _uiState.asStateFlow()

    init {
        safeLaunch {
            val device = deviceRepository.getDevice(deviceId)
            _uiState.value = _uiState.value.copy(device = device)
        }
    }

    fun setLiveMode(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(liveModeRequested = enabled)
    }

    fun clearLiveGraph() {
        _uiState.value = _uiState.value.copy(livePoints = emptyList())
    }

    /** Called directly from [com.smartplug.app.ui.components.PollWhileVisible]'s own coroutine on
     * every poll tick, not via [safeLaunch] — that shared component already guards every caller
     * against an uncaught exception here, so this only needs to convert failures into UI state. */
    suspend fun refresh() {
        val device = _uiState.value.device ?: deviceRepository.getDevice(deviceId) ?: return
        val statusResult = deviceRepository.fetchStatus(device)
        val measurementResult = deviceRepository.fetchMeasurement(device)

        val successfulMeasurement = (measurementResult as? ApiResult.Success)?.value
        if (successfulMeasurement != null) recordMeasurement(device.deviceId, successfulMeasurement)
        val updatedLivePoints = successfulMeasurement?.let { appendLivePoint(_uiState.value.livePoints, it) }
            ?: _uiState.value.livePoints
        val rates = measurementPresentation(updatedLivePoints)
        val isStableForMinute = isStableForMinute(updatedLivePoints, statusResult, successfulMeasurement)
        val vampireSuspected = isVampireEnergySuspected(updatedLivePoints, statusResult)
        val detectedLoad = successfulMeasurement?.let { measurement ->
            findMatchingLoad(device.deviceId, measurement)
        }
        _uiState.value = _uiState.value.copy(
            device = device,
            // A failed poll means this device is unreachable now. Do not retain an old successful
            // status: that made a powered-off SmartPlug appear online indefinitely.
            status = (statusResult as? ApiResult.Success)?.value,
            statusReceivedAtMs = if (statusResult is ApiResult.Success) System.currentTimeMillis() else 0L,
            measurement = successfulMeasurement ?: _uiState.value.measurement,
            livePoints = updatedLivePoints,
            energyPerMinuteWh = rates?.energyKwhPerMinute?.times(1000.0),
            ratesPerMinute = rates,
            canSaveLoadSignature = isStableForMinute,
            detectedLoadName = detectedLoad,
            vampireEnergySuspected = vampireSuspected,
            lastError = ((statusResult as? ApiResult.Failure)?.error ?: (measurementResult as? ApiResult.Failure)?.error)
                ?.let(::describeError),
        )
    }

    /** A standby pattern, not a statement that the load is faulty: 0.5–10 W,
     * stable and continuously present for fifteen minutes while relay is on. */
    private fun isVampireEnergySuspected(
        points: List<LiveMeasurementPoint>,
        status: ApiResult<DeviceStatus>,
    ): Boolean {
        if ((status as? ApiResult.Success)?.value?.relayState != RelayState.ON) return false
        val newest = points.lastOrNull() ?: return false
        val window = points.filter { newest.timestampMs - it.timestampMs <= VAMPIRE_WINDOW_MS }
        if (window.size < 2 || newest.timestampMs - window.first().timestampMs < VAMPIRE_WINDOW_MS) return false
        val watts = window.map { it.measurement.activePowerW }
        val average = watts.average()
        return average in 0.5..10.0 && watts.max() - watts.min() <= maxOf(1.0, average * 0.20)
    }

    /**
     * The voltage/PF secondary labels are a +/- variation, not a before/after change:
     * half the observed range divided by the window average.  For 219, 220, 221 V this is
     * +/- (1 / 220 * 100) = +/- 0.45%.
     */
    private fun measurementPresentation(points: List<LiveMeasurementPoint>): MeasurementRates? {
        val newest = points.lastOrNull() ?: return null
        // kWh/min is intentionally immediate.  The cumulative Wh counter advances in CF pulses,
        // so calculating it from the current active power avoids a visually frozen rate at light loads.
        val immediateKwhPerMinute = (newest.measurement.activePowerW / 60_000.0).coerceAtLeast(0.0)
        val minuteWindow = points.filter { newest.timestampMs - it.timestampMs <= TimeUnit.MINUTES.toMillis(1) }
        if (minuteWindow.size < 2 || newest.timestampMs - minuteWindow.first().timestampMs < TimeUnit.MINUTES.toMillis(1)) {
            return MeasurementRates(0.0, 0.0, 0.0, immediateKwhPerMinute)
        }
        val elapsedMinutes = (newest.timestampMs - minuteWindow.first().timestampMs).toDouble() / TimeUnit.MINUTES.toMillis(1)
        return MeasurementRates(
            voltageVariationPercent = variationPercent(minuteWindow.map { it.measurement.voltageV }),
            activePowerW = (newest.measurement.activePowerW - minuteWindow.first().measurement.activePowerW) / elapsedMinutes,
            powerFactorVariationPercent = variationPercent(minuteWindow.map { it.measurement.powerFactor }),
            energyKwhPerMinute = immediateKwhPerMinute,
        )
    }

    private fun variationPercent(values: List<Double>): Double {
        if (values.isEmpty()) return 0.0
        val average = values.average()
        if (average == 0.0) return 0.0
        return ((values.max() - values.min()) / 2.0 / kotlin.math.abs(average) * 100.0).coerceAtLeast(0.0)
    }

    private fun isStableForMinute(
        points: List<LiveMeasurementPoint>,
        status: ApiResult<DeviceStatus>,
        measurement: ElectricalMeasurement?,
    ): Boolean {
        if ((status as? ApiResult.Success)?.value?.relayState != RelayState.ON || measurement == null || measurement.currentA <= 0.0) return false
        val newest = points.lastOrNull() ?: return false
        val window = points.filter { newest.timestampMs - it.timestampMs <= TimeUnit.MINUTES.toMillis(1) }
        if (window.size < 2 || newest.timestampMs - window.first().timestampMs < TimeUnit.MINUTES.toMillis(1)) return false
        val values = window.map { it.measurement.currentA }
        val average = values.average()
        return average > 0.0 && values.max() - values.min() <= maxOf(0.02, average * 0.08)
    }

    private suspend fun findMatchingLoad(deviceId: String, measurement: ElectricalMeasurement): String? {
        if (measurement.currentA <= 0.0) return null
        return loadSignatureDao.forDevice(deviceId)
            .filter { signature ->
                kotlin.math.abs(signature.currentA - measurement.currentA) <= maxOf(0.03, signature.currentA * 0.10) &&
                    kotlin.math.abs(signature.powerFactor - measurement.powerFactor) <= 0.08
            }
            .minByOrNull { kotlin.math.abs(it.currentA - measurement.currentA) }
            ?.name
    }

    private suspend fun recordMeasurement(deviceId: String, measurement: ElectricalMeasurement) {
        // Keep a durable, lightweight one-minute local history. The dense one-second series is
        // intentionally in-memory only and is used solely for the 1–60 minute live chart.
        val minute = System.currentTimeMillis() / TimeUnit.MINUTES.toMillis(1)
        if (minute == lastStoredMinute) return
        lastStoredMinute = minute
        historyDao.upsertAll(
            listOf(
                HistoryPointEntity(
                    deviceId = deviceId,
                    resolution = LOCAL_HISTORY_RESOLUTION,
                    timestampUtcMs = minute * TimeUnit.MINUTES.toMillis(1),
                    voltageV = measurement.voltageV,
                    currentA = measurement.currentA,
                    activePowerW = measurement.activePowerW,
                    apparentPowerVa = measurement.apparentPowerVa,
                    powerFactor = measurement.powerFactor,
                    energyWh = measurement.energyWh,
                ),
            ),
        )
        historyDao.deleteOlderThan(LOCAL_HISTORY_RESOLUTION, System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30))
    }

    private fun appendLivePoint(
        existing: List<LiveMeasurementPoint>,
        measurement: ElectricalMeasurement,
    ): List<LiveMeasurementPoint> {
        val now = System.currentTimeMillis()
        return (existing + LiveMeasurementPoint(now, measurement)).filter { it.timestampMs >= now - TimeUnit.MINUTES.toMillis(60) }
    }

    fun toggleRelay() {
        val currentState = _uiState.value.status?.relayState ?: RelayState.UNKNOWN
        setRelay(currentState != RelayState.ON)
    }

    fun setRelay(targetOn: Boolean) {
        val device = _uiState.value.device ?: return

        safeLaunch(onError = {
            _uiState.value = _uiState.value.copy(
                relayUiState = RelayUiState.ERROR,
                lastError = "Terjadi kesalahan tak terduga saat mengontrol relay.",
            )
        }) {
            _uiState.value = _uiState.value.copy(relayUiState = RelayUiState.SENDING, lastError = null)
            when (val result = relayRepository.setRelay(device, targetOn)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(relayUiState = RelayUiState.WAITING_SETTLE)
                    val targetState = if (targetOn) RelayState.ON else RelayState.OFF
                    when (val settled = relayRepository.awaitSettledState(device, result.value.commandId, targetState)) {
                        is ApiResult.Success -> {
                            val finalStatus = _uiState.value.status?.copy(relayState = settled.value.state)
                            _uiState.value = _uiState.value.copy(
                                status = finalStatus,
                                relayUiState = if (settled.value.status == RelayCommandStatus.COMPLETED) RelayUiState.IDLE else RelayUiState.ERROR,
                                lastError = if (settled.value.status != RelayCommandStatus.COMPLETED) {
                                    "Perintah relay tidak selesai (${settled.value.status})"
                                } else null,
                            )
                        }
                        is ApiResult.Failure -> _uiState.value = _uiState.value.copy(
                            relayUiState = RelayUiState.ERROR,
                            lastError = describeError(settled.error),
                        )
                    }
                }
                is ApiResult.Failure -> _uiState.value = _uiState.value.copy(
                    relayUiState = RelayUiState.ERROR,
                    lastError = describeError(result.error),
                )
            }
            // Always re-sync from the source of truth rather than trusting our own optimistic state.
            refresh()
        }
    }

    fun renameDevice(newName: String) {
        safeLaunch {
            deviceRepository.renameDevice(deviceId, newName)
            _uiState.value = _uiState.value.copy(device = _uiState.value.device?.copy(displayName = newName))
        }
    }

    fun saveLoadSignature(name: String) {
        val measurement = _uiState.value.measurement ?: return
        if (!_uiState.value.canSaveLoadSignature || name.isBlank()) return
        safeLaunch {
            loadSignatureDao.upsert(
                LoadSignatureEntity(deviceId = deviceId, name = name.trim(), currentA = measurement.currentA, powerFactor = measurement.powerFactor),
            )
            _uiState.value = _uiState.value.copy(detectedLoadName = name.trim())
        }
    }

    fun removeDevice() {
        safeLaunch {
            deviceRepository.removeDevice(deviceId)
            _uiState.value = _uiState.value.copy(isRemoved = true)
        }
    }

    fun resetEnergy() = runControl("Reset energi ditolak") { device -> deviceControlRepository.resetEnergy(device) }

    fun factoryReset() = runControl("Factory reset ditolak") { device -> deviceControlRepository.factoryReset(device) }

    fun applyTimer(days: Int, hours: Int, minutes: Int, seconds: Int) =
        runControl("Timer tidak dapat diterapkan") { device ->
            deviceControlRepository.applyTimer(device, days, hours, minutes, seconds)
        }

    fun resetTimer() = runControl("Timer tidak dapat direset") { device -> deviceControlRepository.resetTimer(device) }

    private fun runControl(fallback: String, action: suspend (SmartPlugDevice) -> ApiResult<Unit>) {
        val device = _uiState.value.device ?: return
        safeLaunch(onError = {
            _uiState.value = _uiState.value.copy(lastError = fallback)
        }) {
            when (val result = action(device)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(lastError = null)
                    refresh()
                }
                is ApiResult.Failure -> _uiState.value = _uiState.value.copy(lastError = describeError(result.error))
            }
        }
    }

    private fun describeError(failure: ApiFailure): String = when (failure.errorCode) {
        "invalid_owner_token" -> "Sesi tidak valid. Ulangi pemasangan SmartPlug ini."
        "measurement_unavailable" -> "Pengukuran belum tersedia dari perangkat."
        "network_timeout" -> "Waktu tunggu jaringan habis. Periksa koneksi Wi-Fi."
        "no_connectivity" -> "Tidak dapat menjangkau perangkat di jaringan."
        "missing_lan_ip", "missing_server_host" -> "Alamat perangkat belum diketahui."
        "relay_busy" -> "Perintah relay sebelumnya masih diproses."
        "relay_actuation_disabled" -> "Kontrol relay dinonaktifkan pada firmware ini."
        "triple_confirmation_required" -> "Konfirmasi tiga tahap belum lengkap."
        "invalid_timer_duration" -> "Durasi timer tidak valid."
        else -> failure.message ?: "Terjadi kesalahan (${failure.errorCode})"
    }

    private companion object {
        const val LOCAL_HISTORY_RESOLUTION = "local_1m"
        val VAMPIRE_WINDOW_MS = TimeUnit.MINUTES.toMillis(15)
    }
}
