package com.smartplug.app.ui.screens.addplug

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartplug.app.data.local.SecureTokenStore
import com.smartplug.app.data.remote.ApiResult
import com.smartplug.app.domain.model.DiscoveredSmartPlugAp
import com.smartplug.app.domain.model.IntegrationMode
import com.smartplug.app.domain.model.PairingFailureReason
import com.smartplug.app.domain.model.PairingState
import com.smartplug.app.domain.model.ServerConnectionProfile
import com.smartplug.app.domain.model.SmartPlugDevice
import com.smartplug.app.domain.repository.DeviceRepository
import com.smartplug.app.domain.repository.DiscoveryRepository
import com.smartplug.app.domain.repository.PairingRepository
import com.smartplug.app.domain.repository.WifiOnboardingRepository
import com.smartplug.app.util.safeLaunch
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/** design.md "Profil MQTT server selalu menggunakan username SmartPlug dan password
 * deviotsolution" — fixed system credentials, not something the user enters. */
private const val SERVER_MQTT_USERNAME = "SmartPlug"
private const val SERVER_MQTT_PASSWORD = "deviotsolution"
private const val SERVER_DISCOVERY_TIMEOUT_MS = 4_000L
private const val HOME_WIFI_RESTORE_TIMEOUT_MS = 20_000L

@HiltViewModel
class AddSmartPlugViewModel @Inject constructor(
    private val wifiOnboardingRepository: WifiOnboardingRepository,
    private val pairingRepository: PairingRepository,
    private val discoveryRepository: DiscoveryRepository,
    private val deviceRepository: DeviceRepository,
    private val tokenStore: SecureTokenStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddSmartPlugUiState())
    val uiState: StateFlow<AddSmartPlugUiState> = _uiState.asStateFlow()

    init {
        startScan()
    }

    /** Every onboarding step talks to real Wi-Fi/HTTP/NSD APIs whose failure modes aren't fully
     * enumerable; routing them through [safeLaunch] guarantees the user always lands on the
     * FAILED screen (with a retry) instead of a force-close, no matter what goes wrong. */
    private fun launchSafely(block: suspend () -> Unit) = safeLaunch(
        onError = { e ->
            _uiState.value = _uiState.value.copy(
                step = OnboardingStep.FAILED,
                errorMessage = "Terjadi kesalahan tak terduga: ${e.message ?: e::class.simpleName}",
            )
        },
        block = block,
    )

    fun startScan() {
        if (!wifiOnboardingRepository.hasRequiredPermission()) {
            _uiState.value = _uiState.value.copy(permissionMissing = true, locationServiceDisabled = false)
            return
        }
        if (wifiOnboardingRepository.isLocationServiceRequiredAndDisabled()) {
            _uiState.value = _uiState.value.copy(
                permissionMissing = false,
                locationServiceDisabled = true,
            )
            return
        }
        _uiState.value = _uiState.value.copy(
            step = OnboardingStep.SCANNING_DEVICES,
            permissionMissing = false,
            locationServiceDisabled = false,
            isScanningDevices = true,
            errorMessage = null,
        )
        launchSafely {
            wifiOnboardingRepository.scanForSmartPlugAps()
                .onSuccess { aps ->
                    val registeredApUnitIds = deviceRepository.observeDevices().first()
                        .mapNotNull { it.apUnitId }
                        .toSet()
                    val unregisteredAps = aps.filterNot { it.unitId in registeredApUnitIds }
                    _uiState.value = _uiState.value.copy(discoveredAps = unregisteredAps, isScanningDevices = false)
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Pemindaian Wi-Fi gagal: ${e.message}",
                        isScanningDevices = false,
                    )
                }
        }
    }

    fun onPermissionGranted() = startScan()

    fun selectAp(ap: DiscoveredSmartPlugAp) {
        _uiState.value = _uiState.value.copy(selectedAp = ap, step = OnboardingStep.CONNECTING_TO_DEVICE, errorMessage = null)
        launchSafely {
            val connectResult = wifiOnboardingRepository.connectToAp(ap.ssid, ap.apPassword)
            if (connectResult.isFailure) {
                _uiState.value = _uiState.value.copy(
                    step = OnboardingStep.FAILED,
                    errorMessage = "Tidak dapat terhubung ke ${ap.ssid}. Pastikan perangkat masih dalam mode setup.",
                )
                return@launchSafely
            }
            when (val infoResult = pairingRepository.fetchPairInfo()) {
                is ApiResult.Success -> {
                    val info = infoResult.value
                    if (info.product != "smartplug" || info.protocol != "pairing-v1") {
                        _uiState.value = _uiState.value.copy(
                            step = OnboardingStep.FAILED,
                            errorMessage = "Perangkat tidak dikenali sebagai SmartPlug yang didukung.",
                        )
                        return@launchSafely
                    }
                    _uiState.value = _uiState.value.copy(
                        pairingToken = info.pairingToken,
                        expectedDeviceId = info.deviceId,
                        step = OnboardingStep.SCANNING_HOME_WIFI,
                    )
                    scanHomeWifi()
                }
                is ApiResult.Failure -> _uiState.value = _uiState.value.copy(
                    step = OnboardingStep.FAILED,
                    errorMessage = "Tidak dapat membaca identitas perangkat (${infoResult.error.errorCode}).",
                )
            }
        }
    }

    private fun scanHomeWifi() {
        val token = _uiState.value.pairingToken ?: return
        launchSafely {
            when (val result = pairingRepository.scanHomeWifi(token)) {
                is ApiResult.Success -> _uiState.value = _uiState.value.copy(
                    // One SSID can be announced by several APs in a mesh/repeater.
                    // Keeping all of them used to feed duplicate LazyColumn keys and
                    // crash exactly when the home Wi-Fi chooser was composed.
                    homeWifiNetworks = result.value
                        .groupBy { it.ssid }
                        .map { (_, networks) -> networks.maxBy { it.rssi } }
                        .sortedByDescending { it.rssi },
                    step = OnboardingStep.CHOOSING_HOME_WIFI,
                )
                is ApiResult.Failure -> _uiState.value = _uiState.value.copy(
                    step = OnboardingStep.FAILED,
                    errorMessage = "Pemindaian Wi-Fi rumah gagal (${result.error.errorCode}).",
                )
            }
        }
        // ServerSmartPlug discovery (NSD/mDNS) is disabled for now: on at least one real device
        // it caused a crash that survived every JVM-level exception guard above, which points to
        // a platform/NSD-stack-level failure (e.g. native), not something try/catch can stop.
        // Nobody in this flow has a ServerSmartPlug anyway; direct REST mode never needed this
        // call. Re-enable only after that's root-caused on a device that can reproduce it.
    }

    fun selectSsid(ssid: String) {
        val savedPassword = tokenStore.savedWifiPassword(ssid).orEmpty()
        _uiState.value = _uiState.value.copy(selectedSsid = ssid, homeWifiPassword = savedPassword)
    }

    fun setHomeWifiPassword(password: String) {
        _uiState.value = _uiState.value.copy(homeWifiPassword = password)
    }

    fun setUseServer(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(useServer = enabled)
    }

    fun toggleHomeWifiPasswordVisible() {
        _uiState.value = _uiState.value.copy(homeWifiPasswordVisible = !_uiState.value.homeWifiPasswordVisible)
    }

    fun confirmAndConnect() {
        val state = _uiState.value
        val token = state.pairingToken ?: return
        val ssid = state.selectedSsid ?: return
        val deviceId = state.expectedDeviceId ?: return
        val password = state.homeWifiPassword

        // Clear the on-screen field the instant "Hubungkan" is tapped, not after the request
        // finishes — the password has already been captured above for the actual API call.
        _uiState.value = state.copy(
            step = OnboardingStep.CONFIGURING,
            errorMessage = null,
            homeWifiPassword = "",
            homeWifiPasswordVisible = false,
        )
        launchSafely {
            val configureResult = if (state.useServer && state.discoveredServer != null) {
                pairingRepository.configureWithServer(
                    pairingToken = token,
                    ssid = ssid,
                    password = password,
                    serverProfile = ServerConnectionProfile(
                        serverId = state.discoveredServer.serverId,
                        brokerHost = state.discoveredServer.host,
                        brokerPort = state.discoveredServer.port,
                        mqttUsername = SERVER_MQTT_USERNAME,
                        mqttPassword = SERVER_MQTT_PASSWORD,
                        baseTopic = "smartplug/$deviceId",
                    ),
                )
            } else {
                pairingRepository.configureDirect(token, ssid, password)
            }

            when (configureResult) {
                is ApiResult.Success -> {
                    tokenStore.saveWifiPassword(ssid, state.homeWifiPassword)
                    _uiState.value = _uiState.value.copy(
                        configurationId = configureResult.value,
                        step = OnboardingStep.WAITING_FOR_CONNECTION,
                    )
                    watchPairingStatus(token, configureResult.value)
                }
                is ApiResult.Failure -> _uiState.value = _uiState.value.copy(
                    step = OnboardingStep.FAILED,
                    errorMessage = "Konfigurasi gagal (${configureResult.error.errorCode}).",
                )
            }
        }
    }

    private fun watchPairingStatus(token: String, configurationId: String) {
        launchSafely {
            pairingRepository.pollStatus(token, configurationId).collect { result ->
                when (result) {
                    is ApiResult.Success -> when (result.value.state) {
                        PairingState.CONNECTED -> finalizeOnboarding(
                            deviceId = result.value.deviceId ?: _uiState.value.expectedDeviceId.orEmpty(),
                            staMac = result.value.staMac.orEmpty(),
                            lanIp = result.value.lanIp,
                            ownerToken = result.value.ownerToken,
                        )
                        PairingState.FAILED -> _uiState.value = _uiState.value.copy(
                            step = OnboardingStep.FAILED,
                            errorMessage = describeFailure(result.value.failureReason),
                        )
                        else -> Unit // CONNECTING/PAIRING/UNPROVISIONED: keep waiting.
                    }
                    is ApiResult.Failure -> _uiState.value = _uiState.value.copy(
                        step = OnboardingStep.FAILED,
                        errorMessage = "Kehilangan koneksi ke perangkat selama proses (${result.error.errorCode}).",
                    )
                }
            }
        }
    }

    private suspend fun finalizeOnboarding(deviceId: String, staMac: String, lanIp: String?, ownerToken: String?) {
        if (lanIp == null || ownerToken == null) {
            _uiState.value = _uiState.value.copy(
                step = OnboardingStep.FAILED,
                errorMessage = "Perangkat terhubung tetapi tidak memberi alamat IP/token yang valid.",
            )
            return
        }
        tokenStore.setOwnerToken(deviceId, ownerToken)
        _uiState.value = _uiState.value.copy(step = OnboardingStep.VERIFYING)

        wifiOnboardingRepository.releaseApBinding()
        val restored = wifiOnboardingRepository.awaitHomeWifiRestored(HOME_WIFI_RESTORE_TIMEOUT_MS)
        if (!restored) {
            _uiState.value = _uiState.value.copy(
                step = OnboardingStep.FAILED,
                errorMessage = "HP belum tersambung kembali ke Wi-Fi rumah. Sambungkan HP ke Wi-Fi " +
                    "yang sama dengan yang baru saja dikonfigurasi, lalu coba lagi.",
            )
            return
        }

        val state = _uiState.value
        val displayName = state.resultDisplayName.ifBlank { "SmartPlug ${deviceId.takeLast(4)}" }
        val device = SmartPlugDevice(
            deviceId = deviceId,
            staMac = staMac,
            displayName = displayName,
            lanIp = lanIp,
            integrationMode = if (state.useServer && state.discoveredServer != null) IntegrationMode.SERVER else IntegrationMode.DIRECT,
            serverId = state.discoveredServer?.serverId,
            serverHost = state.discoveredServer?.host,
            serverPort = state.discoveredServer?.port ?: 80,
            apUnitId = state.selectedAp?.unitId,
        )

        when (val verify = deviceRepository.verifyDeviceIdentity(device)) {
            is ApiResult.Success -> if (verify.value) {
                deviceRepository.saveDevice(device)
                _uiState.value = _uiState.value.copy(
                    step = OnboardingStep.SUCCESS,
                    resultDeviceId = deviceId,
                    resultLanIp = lanIp,
                    resultDisplayName = displayName,
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    step = OnboardingStep.FAILED,
                    errorMessage = "device_id di jaringan rumah tidak cocok dengan hasil pairing.",
                )
            }
            is ApiResult.Failure -> _uiState.value = _uiState.value.copy(
                step = OnboardingStep.FAILED,
                errorMessage = "Tidak dapat memverifikasi perangkat di Wi-Fi rumah (${verify.error.errorCode}).",
            )
        }
    }

    fun setResultDisplayName(name: String) {
        _uiState.value = _uiState.value.copy(resultDisplayName = name)
    }

    /** The device was initially saved before the success screen appears. Persist the final
     * user-editable name before navigation, otherwise the monitoring screen reloads the old
     * automatic name from Room. */
    fun finishWithResultName(onSaved: (String) -> Unit) {
        val deviceId = _uiState.value.resultDeviceId ?: return
        val name = _uiState.value.resultDisplayName.trim()
        if (name.isBlank()) return
        launchSafely {
            deviceRepository.renameDevice(deviceId, name)
            _uiState.value = _uiState.value.copy(resultDisplayName = name)
            onSaved(deviceId)
        }
    }

    fun retry() {
        wifiOnboardingRepository.releaseApBinding()
        _uiState.value = AddSmartPlugUiState()
        startScan()
    }

    private fun describeFailure(reason: PairingFailureReason?): String = when (reason) {
        PairingFailureReason.PAIRING_CLOSED -> "Pemasangan ditutup. Jalankan factory reset pada perangkat."
        PairingFailureReason.INVALID_PAIRING_TOKEN -> "Sesi pemasangan kedaluwarsa. Mulai lagi dari pemilihan perangkat."
        PairingFailureReason.WIFI_NOT_FOUND -> "Wi-Fi rumah tidak ditemukan oleh perangkat."
        PairingFailureReason.WIFI_AUTHENTICATION_FAILED -> "Password Wi-Fi rumah salah."
        PairingFailureReason.WIFI_CONNECTION_TIMEOUT -> "Perangkat gagal terhubung ke Wi-Fi rumah. Coba lagi."
        PairingFailureReason.SERVER_PROFILE_INVALID -> "Konfigurasi ServerSmartPlug tidak valid."
        PairingFailureReason.BROKER_CONNECTION_FAILED -> "Wi-Fi berhasil tetapi ServerSmartPlug belum dapat dihubungi."
        else -> "Pemasangan gagal karena alasan yang tidak diketahui."
    }

    override fun onCleared() {
        super.onCleared()
        if (_uiState.value.step != OnboardingStep.SUCCESS) {
            wifiOnboardingRepository.releaseApBinding()
        }
    }
}
