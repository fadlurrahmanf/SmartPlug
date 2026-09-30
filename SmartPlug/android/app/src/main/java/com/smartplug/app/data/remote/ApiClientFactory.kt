package com.smartplug.app.data.remote

import com.squareup.moshi.Moshi
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SmartPlug's own IP/hostname changes per device and ServerSmartPlug's per install, so Retrofit
 * clients can't be bound to one fixed base URL at DI-graph build time. This factory builds one
 * Retrofit instance per base URL and reuses it (LAN calls are frequent — polling every 1-2s —
 * so avoiding client re-creation on every request matters).
 *
 * Timeouts follow design.md's "Interval pembacaan pengukuran": 8s call timeout, and callers own
 * their own retry/backoff (see [com.smartplug.app.util.RetryPolicy]) rather than OkHttp retrying
 * silently underneath them.
 */
@Singleton
open class ApiClientFactory @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val moshi: Moshi,
) {
    private val retrofitCache = ConcurrentHashMap<String, Retrofit>()

    // open: PairingRepositoryImplTest overrides this to redirect to a MockWebServer standing in
    // for the SmartPlug AP, since the real pairing endpoints aren't implemented by firmware yet.
    open fun pairingApi(baseUrl: String = PAIRING_BASE_URL): PairingApi =
        retrofitFor(baseUrl).create(PairingApi::class.java)

    fun deviceApi(baseUrl: String): DeviceApi =
        retrofitFor(baseUrl).create(DeviceApi::class.java)

    fun serverApi(baseUrl: String): ServerApi =
        retrofitFor(baseUrl).create(ServerApi::class.java)

    fun serverSetupApi(baseUrl: String = PAIRING_BASE_URL): ServerSetupApi =
        retrofitFor(baseUrl).create(ServerSetupApi::class.java)

    private fun retrofitFor(baseUrl: String): Retrofit {
        val normalized = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        return retrofitCache.getOrPut(normalized) {
            Retrofit.Builder()
                .baseUrl(normalized)
                .client(okHttpClient.newBuilder()
                    .callTimeout(CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .build())
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
        }
    }

    /** Drops cached clients for a base URL, e.g. after a device's `lan_ip` changes. */
    fun invalidate(baseUrl: String) {
        val normalized = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        retrofitCache.remove(normalized)
    }

    companion object {
        const val PAIRING_BASE_URL = "http://192.168.4.1/"
        private const val CALL_TIMEOUT_SECONDS = 8L

        fun lanBaseUrl(ip: String): String = "http://$ip/"
        fun hostBaseUrl(host: String, port: Int = 80): String =
            if (port == 80) "http://$host/" else "http://$host:$port/"
    }
}
