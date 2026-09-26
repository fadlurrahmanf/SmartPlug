package com.smartplug.app.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartplug.app.data.local.AppPreferences
import com.smartplug.app.data.local.AppSettings
import com.smartplug.app.data.local.AppThemeMode
import com.smartplug.app.data.local.SecureTokenStore
import com.smartplug.app.data.local.db.HistoryDao
import com.smartplug.app.data.local.db.LoadSignatureDao
import com.smartplug.app.data.remote.ApiResult
import com.smartplug.app.domain.repository.DeviceControlRepository
import com.smartplug.app.domain.repository.DeviceRepository
import com.smartplug.app.ui.localization.AppLanguage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import javax.inject.Inject

data class AppResetUiState(
    val isRunning: Boolean = false,
    val summary: String? = null,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val appPreferences: AppPreferences,
    private val deviceRepository: DeviceRepository,
    private val deviceControlRepository: DeviceControlRepository,
    private val historyDao: HistoryDao,
    private val loadSignatureDao: LoadSignatureDao,
    private val secureTokenStore: SecureTokenStore,
) : ViewModel() {

    val settings: StateFlow<AppSettings> = appPreferences.settings.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings(),
    )
    private val _appResetState = kotlinx.coroutines.flow.MutableStateFlow(AppResetUiState())
    val appResetState: StateFlow<AppResetUiState> = _appResetState

    fun setSoundEnabled(enabled: Boolean) = viewModelScope.launch { appPreferences.setSoundEnabled(enabled) }
    fun setHapticEnabled(enabled: Boolean) = viewModelScope.launch { appPreferences.setHapticEnabled(enabled) }
    fun setThemeMode(mode: AppThemeMode) = viewModelScope.launch { appPreferences.setThemeMode(mode) }
    fun setLanguage(language: AppLanguage) = viewModelScope.launch { appPreferences.setLanguage(language) }

    /**
     * A device is removed locally only after its own factory-reset endpoint accepted the
     * triple-confirmed request.  If it is offline/fails, it remains visible so the user has a
     * way to retry instead of silently losing control of an unreset device.
     */
    fun resetAppAndRegisteredDevices() = viewModelScope.launch {
        if (_appResetState.value.isRunning) return@launch
        _appResetState.value = AppResetUiState(isRunning = true)
        val devices = deviceRepository.observeDevices().first()
        val failedNames = mutableListOf<String>()
        var resetCount = 0
        devices.forEach { device ->
            when (deviceControlRepository.factoryReset(device)) {
                is ApiResult.Success -> {
                    historyDao.clearForDevice(device.deviceId)
                    loadSignatureDao.clearForDevice(device.deviceId)
                    deviceRepository.removeDevice(device.deviceId)
                    resetCount += 1
                }
                is ApiResult.Failure -> failedNames += device.displayName
            }
        }
        if (failedNames.isEmpty()) {
            secureTokenStore.clearAll()
            appPreferences.reset()
            _appResetState.value = AppResetUiState(
                summary = if (devices.isEmpty()) "App data reset." else "$resetCount device(s) factory-reset and app data cleared.",
            )
        } else {
            _appResetState.value = AppResetUiState(
                summary = "$resetCount device(s) reset. Still unavailable: ${failedNames.joinToString()}.",
            )
        }
    }
}
