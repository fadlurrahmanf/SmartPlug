package com.smartplug.app.ui.screens.devices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartplug.app.data.remote.ApiResult
import com.smartplug.app.domain.model.DeviceStatus
import com.smartplug.app.domain.model.ElectricalMeasurement
import com.smartplug.app.domain.model.SmartPlugDevice
import com.smartplug.app.domain.repository.DeviceRepository
import com.smartplug.app.util.safeLaunch
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DeviceRow(
    val device: SmartPlugDevice,
    val status: DeviceStatus?,
    val measurement: ElectricalMeasurement?,
    val online: Boolean,
)

data class DeviceListUiState(
    val rows: List<DeviceRow> = emptyList(),
    val isRefreshing: Boolean = false,
)

@HiltViewModel
class DeviceListViewModel @Inject constructor(
    private val deviceRepository: DeviceRepository,
) : ViewModel() {

    private val statuses = MutableStateFlow<Map<String, DeviceStatus>>(emptyMap())
    private val measurements = MutableStateFlow<Map<String, ElectricalMeasurement>>(emptyMap())
    private val refreshing = MutableStateFlow(false)

    val uiState: StateFlow<DeviceListUiState> = combine(
        deviceRepository.observeDevices(),
        statuses,
        measurements,
        refreshing,
    ) { devices, statusMap, measurementMap, isRefreshing ->
        DeviceListUiState(
            rows = devices.map { device ->
                val status = statusMap[device.deviceId]
                DeviceRow(device = device, status = status, measurement = measurementMap[device.deviceId], online = status?.fresh == true)
            },
            isRefreshing = isRefreshing,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DeviceListUiState())

    fun refreshAll() {
        safeLaunch {
            refreshing.value = true
            try {
                val devices = uiState.value.rows.map { it.device }
                val results = devices.map { device ->
                    viewModelScope.async { Triple(device.deviceId, deviceRepository.fetchStatus(device), deviceRepository.fetchMeasurement(device)) }
                }.awaitAll()
                val updated = statuses.value.toMutableMap()
                val updatedMeasurements = measurements.value.toMutableMap()
                results.forEach { (deviceId, statusResult, measurementResult) ->
                    if (statusResult is ApiResult.Success) {
                        updated[deviceId] = statusResult.value
                    } else {
                        // Drop stale success data immediately so the overview is truthful after
                        // a SmartPlug loses power or leaves the LAN.
                        updated.remove(deviceId)
                    }
                    if (measurementResult is ApiResult.Success) updatedMeasurements[deviceId] = measurementResult.value
                    else updatedMeasurements.remove(deviceId)
                }
                statuses.value = updated
                measurements.value = updatedMeasurements
            } finally {
                // Never leave the pull-to-refresh spinner stuck on if any single device's
                // status fetch throws instead of returning ApiResult.Failure.
                refreshing.value = false
            }
        }
    }

    fun removeDevice(deviceId: String) {
        safeLaunch { deviceRepository.removeDevice(deviceId) }
    }
}
