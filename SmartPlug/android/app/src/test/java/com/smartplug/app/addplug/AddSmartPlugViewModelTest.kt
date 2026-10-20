package com.smartplug.app.addplug

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.common.truth.Truth.assertThat
import com.smartplug.app.data.local.SecureTokenStore
import com.smartplug.app.data.remote.ApiResult
import com.smartplug.app.domain.model.ApiFailure
import com.smartplug.app.domain.model.DiscoveredSmartPlugAp
import com.smartplug.app.domain.model.HomeWifiNetwork
import com.smartplug.app.domain.model.IntegrationMode
import com.smartplug.app.domain.model.PairInfo
import com.smartplug.app.domain.model.PairingState
import com.smartplug.app.domain.model.PairingStatus
import com.smartplug.app.domain.model.SmartPlugDevice
import com.smartplug.app.domain.repository.DeviceRepository
import com.smartplug.app.domain.repository.DeviceControlRepository
import com.smartplug.app.domain.repository.DiscoveryRepository
import com.smartplug.app.domain.repository.PairingRepository
import com.smartplug.app.domain.repository.WifiOnboardingRepository
import com.smartplug.app.ui.screens.addplug.AddSmartPlugViewModel
import com.smartplug.app.ui.screens.addplug.OnboardingStep
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Guards the already-working Direct SmartPlug setup path from accidental ServerSmartPlug
 * coupling.  These are JVM tests: the physical AP-to-LAN handoff is covered separately on a
 * phone because Android owns Wi-Fi binding and network selection.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AddSmartPlugViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val dispatcher = StandardTestDispatcher()
    private lateinit var wifi: WifiOnboardingRepository
    private lateinit var pairing: PairingRepository
    private lateinit var devices: DeviceRepository
    private lateinit var deviceControl: DeviceControlRepository
    private lateinit var tokens: SecureTokenStore
    private lateinit var discovery: DiscoveryRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        wifi = mockk(relaxed = true)
        pairing = mockk(relaxed = true)
        devices = mockk(relaxed = true)
        deviceControl = mockk(relaxed = true)
        tokens = mockk(relaxed = true)
        discovery = mockk(relaxed = true)

        every { wifi.hasRequiredPermission() } returns true
        every { wifi.isLocationServiceRequiredAndDisabled() } returns false
        coEvery { wifi.scanForSmartPlugAps() } returns Result.success(emptyList())
        every { tokens.savedWifiPassword(any()) } returns null
        every { devices.observeDevices() } returns flowOf(emptyList())
        coEvery { pairing.fetchPairInfo() } returns ApiResult.Success(pairInfo)
        coEvery { pairing.scanHomeWifi("pair-token") } returns ApiResult.Success(
            listOf(HomeWifiNetwork("Mimi", -42, "secured")),
        )
        coEvery { pairing.configureDirect("pair-token", "Mimi", "home-password") } returns
            ApiResult.Success("config-1")
        coEvery { wifi.awaitHomeWifiRestored(any()) } returns true
        coEvery { devices.verifyDeviceIdentity(any()) } returns ApiResult.Success(true)
        coEvery { deviceControl.setSharedDisplayName(any(), any()) } returns ApiResult.Success(Unit)
        coEvery { pairing.completePairingAt(any(), "pair-token", "owner-token", "config-1") } returns
            ApiResult.Success(Unit)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `successful add SmartPlug always configures and persists Direct without server fields`() = runTest {
        every { pairing.pollStatus("pair-token", "config-1") } returns flowOf(
            ApiResult.Success(connectedStatus),
        )
        val viewModel = AddSmartPlugViewModel(wifi, pairing, devices, deviceControl, tokens, discovery)

        completeDirectSetup(viewModel)

        assertThat(viewModel.uiState.value.step).isEqualTo(OnboardingStep.SUCCESS)
        coVerify(exactly = 1) {
            pairing.configureDirect("pair-token", "Mimi", "home-password")
        }
        coVerify(exactly = 1) {
            devices.saveDevice(match { device ->
                device.deviceId == connectedStatus.deviceId &&
                    device.integrationMode == IntegrationMode.DIRECT &&
                    device.serverId == null &&
                    device.serverHost == null
            })
        }
        coVerify(exactly = 1) { deviceControl.setSharedDisplayName(any(), any()) }
        coVerify(exactly = 1) { pairing.completePairingAt(any(), "pair-token", "owner-token", "config-1") }
        verify(exactly = 1) { pairing.pollStatus("pair-token", "config-1") }
        verify(exactly = 1) { wifi.releaseApBinding() }
    }

    @Test
    fun `AP loss stops AP polling releases binding and uses bounded LAN status recovery`() = runTest {
        every { pairing.pollStatus("pair-token", "config-1") } returns flowOf(
            ApiResult.Failure(ApiFailure(0, "ap_lost")),
        )
        coEvery {
            discovery.resolveDeviceLanIp(connectedStatus.deviceId!!, any())
        } returns "192.168.1.29"
        coEvery {
            pairing.fetchStatusAt(any(), "pair-token", "config-1")
        } returns ApiResult.Success(connectedStatus)
        val viewModel = AddSmartPlugViewModel(wifi, pairing, devices, deviceControl, tokens, discovery)

        completeDirectSetup(viewModel)

        assertThat(viewModel.uiState.value.step).isEqualTo(OnboardingStep.SUCCESS)
        // One release happens before LAN recovery; the second is the normal final handoff
        // performed by finalizeOnboarding after the LAN status reaches CONNECTED.
        verify(exactly = 2) { wifi.releaseApBinding() }
        verify(exactly = 1) { pairing.pollStatus("pair-token", "config-1") }
        coVerify(exactly = 1) {
            discovery.resolveDeviceLanIp(connectedStatus.deviceId!!, 3_000L)
        }
        coVerify(exactly = 1) {
            pairing.fetchStatusAt(any(), "pair-token", "config-1")
        }
        coVerify(exactly = 1) {
            devices.saveDevice(match { it.integrationMode == IntegrationMode.DIRECT && it.serverId == null })
        }
    }

    @Test
    fun `scan hides already paired SmartPlugs and lists only unpaired ones`() = runTest {
        every { devices.observeDevices() } returns flowOf(
            listOf(
                SmartPlugDevice(
                    deviceId = "SP-AABBCCDDEEFF",
                    staMac = "AA:BB:CC:DD:EE:FF",
                    displayName = "Paired",
                    lanIp = "192.168.1.29",
                    integrationMode = IntegrationMode.DIRECT,
                    apUnitId = "1122",
                ),
                // Added through "Existing SmartPlug": no AP id, so it can never hide an AP.
                SmartPlugDevice(
                    deviceId = "SP-112233445566",
                    staMac = "11:22:33:44:55:66",
                    displayName = "Existing",
                    lanIp = null,
                    integrationMode = IntegrationMode.DIRECT,
                    apUnitId = null,
                ),
            ),
        )
        coEvery { wifi.scanForSmartPlugAps() } returns Result.success(
            listOf(
                DiscoveredSmartPlugAp("SP-1122", "1122", -35),
                DiscoveredSmartPlugAp("SP-3344", "3344", -50),
            ),
        )

        val viewModel = AddSmartPlugViewModel(wifi, pairing, devices, deviceControl, tokens, discovery)
        dispatcher.scheduler.advanceUntilIdle()

        assertThat(viewModel.uiState.value.discoveredAps.map { it.unitId }).containsExactly("3344")
    }

    private fun completeDirectSetup(viewModel: AddSmartPlugViewModel) {
        viewModel.selectAp(DiscoveredSmartPlugAp("SP-1122", "1122", -35))
        dispatcher.scheduler.advanceUntilIdle()
        assertThat(viewModel.uiState.value.step).isEqualTo(OnboardingStep.CHOOSING_HOME_WIFI)
        viewModel.selectSsid("Mimi")
        viewModel.setHomeWifiPassword("home-password")
        viewModel.confirmAndConnect()
        dispatcher.scheduler.advanceUntilIdle()
    }

    private companion object {
        val pairInfo = PairInfo(
            apiVersion = "1.0",
            product = "smartplug",
            protocol = "pairing-v1",
            deviceId = "SP-AABBCCDDEEFF",
            staMac = "AA:BB:CC:DD:EE:FF",
            state = "unprovisioned",
            pairingToken = "pair-token",
            tokenExpiresInS = 300,
        )
        val connectedStatus = PairingStatus(
            state = PairingState.CONNECTED,
            deviceId = "SP-AABBCCDDEEFF",
            staMac = "AA:BB:CC:DD:EE:FF",
            lanIp = "192.168.1.29",
            ownerToken = "owner-token",
        )
    }
}
