package com.smartplug.app.pairing

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.smartplug.app.data.remote.ApiClientFactory
import com.smartplug.app.data.remote.ApiResult
import com.smartplug.app.data.repository.PairingRepositoryImpl
import com.smartplug.app.domain.model.PairingFailureReason
import com.smartplug.app.domain.model.PairingState
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * Exercises the SmartPlug pairing state machine against a [MockWebServer] standing in for the
 * device AP, since firmware doesn't implement the `/api/v1/pair` endpoints yet (design.md's own
 * "Status implementasi firmware" admits this) — there is no real device to pair against today.
 */
class PairingRepositoryImplTest {

    private lateinit var server: MockWebServer
    private lateinit var repository: PairingRepositoryImpl

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        repository = PairingRepositoryImpl(
            FixedBaseUrlApiClientFactory(OkHttpClient(), moshi, server.url("/").toString())
        )
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `fetchPairInfo parses device identity`() = runTest {
        server.enqueue(
            MockResponse().setBody(
                """{"api_version":"1.0","product":"smartplug","protocol":"pairing-v1","device_id":"SP-112233445566","sta_mac":"11:22:33:44:55:66","state":"unprovisioned","pairing_token":"abc","token_expires_in_s":300}"""
            )
        )

        val result = repository.fetchPairInfo()

        assertThat(result).isInstanceOf(ApiResult.Success::class.java)
        assertThat((result as ApiResult.Success).value.deviceId).isEqualTo("SP-112233445566")
    }

    @Test
    fun `pollStatus emits connecting then connected and then stops`() = runTest {
        server.enqueue(MockResponse().setBody("""{"api_version":"1.0","state":"connecting"}"""))
        server.enqueue(
            MockResponse().setBody(
                """{"api_version":"1.0","state":"connected","device_id":"SP-112233445566","sta_mac":"11:22:33:44:55:66","lan_ip":"192.168.1.25","owner_token":"tok-1"}"""
            )
        )

        repository.pollStatus("abc", "cfg-1").test {
            val first = awaitItem() as ApiResult.Success
            assertThat(first.value.state).isEqualTo(PairingState.CONNECTING)

            val second = awaitItem() as ApiResult.Success
            assertThat(second.value.state).isEqualTo(PairingState.CONNECTED)
            assertThat(second.value.lanIp).isEqualTo("192.168.1.25")
            assertThat(second.value.ownerToken).isEqualTo("tok-1")

            awaitComplete()
        }
        assertThat(server.requestCount).isEqualTo(2)
    }

    @Test
    fun `pollStatus stops and surfaces the reason on failed`() = runTest {
        server.enqueue(
            MockResponse().setBody("""{"api_version":"1.0","state":"failed","reason":"wifi_authentication_failed"}""")
        )

        repository.pollStatus("abc", "cfg-1").test {
            val result = awaitItem() as ApiResult.Success
            assertThat(result.value.state).isEqualTo(PairingState.FAILED)
            assertThat(result.value.failureReason).isEqualTo(PairingFailureReason.WIFI_AUTHENTICATION_FAILED)
            awaitComplete()
        }
    }

    @Test
    fun `pollStatus stops immediately on a transport failure rather than looping forever`() = runTest {
        server.enqueue(MockResponse().setResponseCode(500))

        repository.pollStatus("abc", "cfg-1").test {
            val result = awaitItem()
            assertThat(result).isInstanceOf(ApiResult.Failure::class.java)
            awaitComplete()
        }
        assertThat(server.requestCount).isEqualTo(1)
    }
}

/** Redirects [ApiClientFactory.pairingApi]'s default base URL to the test [MockWebServer]. */
private class FixedBaseUrlApiClientFactory(
    okHttpClient: OkHttpClient,
    moshi: Moshi,
    private val fixedBaseUrl: String,
) : ApiClientFactory(okHttpClient, moshi) {
    override fun pairingApi(baseUrl: String) = super.pairingApi(fixedBaseUrl)
}
