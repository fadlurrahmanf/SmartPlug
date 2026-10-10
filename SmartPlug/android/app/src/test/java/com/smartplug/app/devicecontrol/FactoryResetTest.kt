package com.smartplug.app.devicecontrol

import com.google.common.truth.Truth.assertThat
import com.smartplug.app.data.local.SecureTokenStore
import com.smartplug.app.data.remote.ApiClientFactory
import com.smartplug.app.data.remote.ApiResult
import com.smartplug.app.data.repository.DeviceControlRepositoryImpl
import com.smartplug.app.domain.model.IntegrationMode
import com.smartplug.app.domain.model.SmartPlugDevice
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Before
import org.junit.Test

class FactoryResetTest {

    private lateinit var server: MockWebServer
    private lateinit var repository: DeviceControlRepositoryImpl
    private val requests = mutableListOf<String>()

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val tokenStore = mockk<SecureTokenStore>()
        every { tokenStore.accessCredential(any()) } returns "owner-token"
        every { tokenStore.ownerToken(any()) } returns "owner-token"
        every { tokenStore.serverApiToken(any()) } returns "server-token"
        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        repository = DeviceControlRepositoryImpl(tokenStore, ApiClientFactory(OkHttpClient(), moshi))
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun device(mode: IntegrationMode) = SmartPlugDevice(
        deviceId = "SP-AABBCCDDEEFF",
        staMac = "AA:BB:CC:DD:EE:FF",
        displayName = "Kulkas",
        lanIp = "localhost:${server.port}",
        integrationMode = mode,
        serverId = if (mode == IntegrationMode.SERVER) "srv-1" else null,
        serverHost = if (mode == IntegrationMode.SERVER) "localhost" else null,
        serverPort = server.port,
    )

    private fun latest(status: String) = MockResponse().setResponseCode(200).setBody(
        """{"device_id":"SP-AABBCCDDEEFF","captured_at_ms":0,"status":"$status","relay_state":"off"}"""
    )

    /** [statusAfterReset] is what ServerSmartPlug reports once its reset command was queued. */
    private fun serve(localReset: Int, statusBefore: String, statusAfterReset: String) {
        var queued = false
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val path = request.path.orEmpty()
                requests += "${request.method} $path"
                return when (path) {
                    "/api/v1/factory-reset" -> MockResponse().setResponseCode(localReset)
                        .setBody(if (localReset == 202) "{}" else """{"error":"no_route"}""")
                    "/api/v1/devices/SP-AABBCCDDEEFF/factory-reset" -> { queued = true; MockResponse().setResponseCode(202).setBody("{}") }
                    "/api/v1/devices/SP-AABBCCDDEEFF/latest" -> latest(if (queued) statusAfterReset else statusBefore)
                    else -> MockResponse().setResponseCode(404)
                }
            }
        }
    }

    @Test
    fun `local acknowledgement is enough and the server is not used`() = runTest {
        serve(localReset = 202, statusBefore = "online", statusAfterReset = "offline")

        val result = repository.factoryReset(device(IntegrationMode.SERVER))

        assertThat(result).isInstanceOf(ApiResult.Success::class.java)
        assertThat(requests).containsExactly("POST /api/v1/factory-reset")
    }

    @Test
    fun `server reset succeeds only after the plug stays offline`() = runTest {
        serve(localReset = 503, statusBefore = "online", statusAfterReset = "offline")

        val result = repository.factoryReset(device(IntegrationMode.SERVER))

        assertThat(result).isInstanceOf(ApiResult.Success::class.java)
        assertThat(requests).contains("POST /api/v1/devices/SP-AABBCCDDEEFF/factory-reset")
    }

    @Test
    fun `server reset is unconfirmed while the plug stays online`() = runTest {
        serve(localReset = 503, statusBefore = "online", statusAfterReset = "online")

        val result = repository.factoryReset(device(IntegrationMode.SERVER))

        assertThat((result as ApiResult.Failure).error.errorCode).isEqualTo("factory_reset_unconfirmed")
    }

    @Test
    fun `server reset is refused when the plug is already offline`() = runTest {
        serve(localReset = 503, statusBefore = "offline", statusAfterReset = "offline")

        val result = repository.factoryReset(device(IntegrationMode.SERVER))

        assertThat((result as ApiResult.Failure).error.errorCode).isEqualTo("device_offline")
        assertThat(requests).doesNotContain("POST /api/v1/devices/SP-AABBCCDDEEFF/factory-reset")
    }

    @Test
    fun `direct reset failure is reported without a server fallback`() = runTest {
        serve(localReset = 503, statusBefore = "online", statusAfterReset = "offline")

        val result = repository.factoryReset(device(IntegrationMode.DIRECT))

        assertThat(result).isInstanceOf(ApiResult.Failure::class.java)
        assertThat(requests).containsExactly("POST /api/v1/factory-reset")
    }
}
