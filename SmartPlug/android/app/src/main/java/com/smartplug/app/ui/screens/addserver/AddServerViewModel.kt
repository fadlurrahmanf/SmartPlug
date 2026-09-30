package com.smartplug.app.ui.screens.addserver

import androidx.lifecycle.ViewModel
import com.smartplug.app.data.local.ServerProfileStore
import com.smartplug.app.data.remote.ApiClientFactory
import com.smartplug.app.data.remote.ApiResult
import com.smartplug.app.data.remote.safeApiCall
import com.smartplug.app.domain.model.RegisteredServer
import com.smartplug.app.domain.repository.WifiOnboardingRepository
import com.smartplug.app.util.safeLaunch
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

data class AddServerUiState(
    val permissionMissing: Boolean = false,
    val locationServiceDisabled: Boolean = false,
    val displayName: String = "ServerSmartPlug",
    val ssid: String = "",
    val password: String = "",
    val apiToken: String = "",
    val mqttUsername: String = "SmartPlug",
    val mqttPassword: String = "deviotsolution",
    val busy: Boolean = false,
    val completed: Boolean = false,
    val message: String? = null,
)

@HiltViewModel
class AddServerViewModel @Inject constructor(
    private val wifi: WifiOnboardingRepository,
    private val clients: ApiClientFactory,
    private val serverProfiles: ServerProfileStore,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AddServerUiState())
    val uiState: StateFlow<AddServerUiState> = _uiState.asStateFlow()

    init { refreshPrerequisites() }

    fun update(transform: (AddServerUiState) -> AddServerUiState) { _uiState.value = transform(_uiState.value) }

    fun refreshPrerequisites() {
        _uiState.value = _uiState.value.copy(
            permissionMissing = !wifi.hasRequiredPermission(),
            locationServiceDisabled = wifi.isLocationServiceRequiredAndDisabled(),
        )
    }

    fun provision() {
        val state = _uiState.value
        if (state.permissionMissing || state.locationServiceDisabled) {
            refreshPrerequisites()
            return
        }
        if (state.ssid.isBlank() || state.apiToken.length < 16 || state.mqttUsername.length < 3 || state.mqttPassword.length < 8) {
            _uiState.value = state.copy(message = "Isi SSID, token API minimal 16 karakter, dan kredensial MQTT yang valid.")
            return
        }
        safeLaunch(onError = { error ->
            wifi.releaseApBinding()
            _uiState.value = _uiState.value.copy(busy = false, message = "Provisioning server gagal: ${error.message ?: "network_error"}")
        }) {
            _uiState.value = state.copy(busy = true, message = "Menghubungkan ke ServerSmartPlug-Setup…")
            val bound = wifi.connectToAp(SETUP_SSID, SETUP_PASSWORD)
            if (bound.isFailure) {
                _uiState.value = _uiState.value.copy(busy = false, message = "Tidak dapat terhubung ke $SETUP_SSID. Pastikan server masih mode setup.")
                return@safeLaunch
            }
            val api = clients.serverSetupApi()
            val before = safeApiCall { api.status() }
            val setup = (before as? ApiResult.Success)?.value
            if (setup == null || setup.serverId.isBlank()) {
                wifi.releaseApBinding()
                _uiState.value = _uiState.value.copy(busy = false, message = "Server setup tidak memberikan identitas yang valid.")
                return@safeLaunch
            }
            _uiState.value = _uiState.value.copy(message = "Mengirim konfigurasi Wi-Fi ke server…")
            when (val result = safeApiCall {
                api.apply(mapOf(
                    "ssid" to state.ssid, "wifi_password" to state.password,
                    "api_token" to state.apiToken, "broker_username" to state.mqttUsername,
                    "broker_password" to state.mqttPassword,
                ))
            }) {
                is ApiResult.Failure -> {
                    wifi.releaseApBinding()
                    _uiState.value = _uiState.value.copy(busy = false, message = "Konfigurasi ditolak: ${result.error.errorCode}")
                }
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(message = "Menunggu server terhubung ke Wi-Fi rumah…")
                    var stationIp = ""
                    for (attempt in 0 until STATION_IP_POLL_COUNT) {
                        delay(STATION_IP_POLL_INTERVAL_MS)
                        val current = safeApiCall { api.status() }
                        stationIp = (current as? ApiResult.Success)?.value?.station?.ip.orEmpty()
                        if (stationIp.isNotBlank()) break
                    }
                    wifi.releaseApBinding()
                    if (stationIp.isBlank()) {
                        _uiState.value = _uiState.value.copy(busy = false, message = "Server menyimpan konfigurasi, tetapi belum mendapat IP Wi-Fi. Periksa SSID/password lalu coba lagi.")
                        return@safeLaunch
                    }
                    serverProfiles.save(RegisteredServer(
                        serverId = setup.serverId, displayName = state.displayName.ifBlank { setup.serverId }, host = stationIp,
                        mqttPort = setup.mqttPort, mqttUsername = state.mqttUsername, mqttPassword = state.mqttPassword,
                    ), state.apiToken)
                    _uiState.value = _uiState.value.copy(busy = false, completed = true, message = "Server ${setup.serverId} tersimpan di aplikasi pada $stationIp.")
                }
            }
        }
    }

    companion object {
        private const val SETUP_SSID = "ServerSmartPlug-Setup"
        private const val SETUP_PASSWORD = "SmartPlugSetup"
        private const val STATION_IP_POLL_COUNT = 20
        private const val STATION_IP_POLL_INTERVAL_MS = 1_000L
    }
}
