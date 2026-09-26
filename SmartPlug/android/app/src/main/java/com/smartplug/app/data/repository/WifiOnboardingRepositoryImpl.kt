package com.smartplug.app.data.repository

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.location.LocationManager
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSpecifier
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import com.smartplug.app.domain.model.DiscoveredSmartPlugAp
import com.smartplug.app.domain.repository.WifiOnboardingRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/** Access-point SSIDs SmartPlug/ServerSmartPlug advertise before onboarding, design.md
 * "Standar nama discovery". Filters out `SrvrPlug-` too so it never shows as a pairable plug. */
private const val SMARTPLUG_AP_PREFIX = "SP-"
private const val SERVER_AP_PREFIX = "SrvrPlug-"

@Singleton
class WifiOnboardingRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : WifiOnboardingRepository {

    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val connectivityManager =
        context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private var boundCallback: ConnectivityManager.NetworkCallback? = null
    private var boundNetwork: Network? = null

    override fun hasRequiredPermission(): Boolean {
        // ACCESS_FINE_LOCATION on every version, not NEARBY_WIFI_DEVICES on 13+: see the manifest
        // comment — the neverForLocation alternative redacts real SSIDs from scan results, which
        // breaks onboarding's whole "find the AP literally named SP-<unit_id>" premise.
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun isLocationServiceRequiredAndDisabled(): Boolean {
        // Applies on every API level when scanning via ACCESS_FINE_LOCATION (unlike the
        // NEARBY_WIFI_DEVICES+neverForLocation path, which is exempt but redacts SSIDs instead).
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return false
        return !LocationManagerCompat.isLocationEnabled(locationManager)
    }

    override suspend fun scanForSmartPlugAps(): Result<List<DiscoveredSmartPlugAp>> {
        if (!hasRequiredPermission()) {
            return Result.failure(SecurityException("Wi-Fi scan permission not granted"))
        }
        if (isLocationServiceRequiredAndDisabled()) {
            return Result.failure(IllegalStateException("location_services_disabled"))
        }
        return try {
            // wifiManager.scanResults alone only returns whatever the OS happened to cache from
            // its own periodic background scanning, which can be empty or stale right after the
            // user grants permission — an SP-<unit_id> AP that only just started broadcasting
            // would silently never show up. A scan must actually be triggered first.
            triggerScanAndAwaitResults()
            var discovered = readDiscoveredAps()
            // Android throttles WifiManager.startScan() aggressively (a handful of calls per app
            // per short window, tighter on newer OS versions) — after a few retries in one
            // onboarding session our own trigger silently stops actually scanning and just
            // returns immediately. Rather than report "not found" off a single stale read, keep
            // polling the OS's own cache for a while longer: Android still runs its own
            // periodic background scan independent of ours, which this can catch instead.
            var attempts = 0
            while (discovered.isEmpty() && attempts < MAX_FALLBACK_POLL_ATTEMPTS) {
                delay(FALLBACK_POLL_INTERVAL_MS)
                discovered = readDiscoveredAps()
                attempts++
            }
            Result.success(discovered)
        } catch (e: SecurityException) {
            Result.failure(e)
        }
    }

    @Suppress("DEPRECATION")
    private fun readDiscoveredAps(): List<DiscoveredSmartPlugAp> = wifiManager.scanResults
        .filter { it.SSID.startsWith(SMARTPLUG_AP_PREFIX) && !it.SSID.startsWith(SERVER_AP_PREFIX) }
        .distinctBy { it.SSID }
        .map { result ->
            DiscoveredSmartPlugAp(
                ssid = result.SSID,
                unitId = result.SSID.removePrefix(SMARTPLUG_AP_PREFIX),
                rssi = result.level,
            )
        }
        .sortedByDescending { it.rssi }

    /** Triggers a fresh scan and suspends until results are ready (or a timeout, or the OS
     * throttles/refuses the scan request — Android limits how often an app may call
     * [WifiManager.startScan]; on throttling this just falls back to whatever scanResults
     * already holds rather than hanging forever waiting for a broadcast that never arrives). */
    private suspend fun triggerScanAndAwaitResults() {
        withTimeoutOrNull(SCAN_TIMEOUT_MS) {
            suspendCancellableCoroutine<Unit> { continuation ->
                val receiver = object : BroadcastReceiver() {
                    override fun onReceive(ctx: Context, intent: Intent) {
                        if (continuation.isActive) continuation.resume(Unit)
                        runCatching { context.unregisterReceiver(this) }
                    }
                }
                ContextCompat.registerReceiver(
                    context,
                    receiver,
                    IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION),
                    ContextCompat.RECEIVER_NOT_EXPORTED,
                )
                continuation.invokeOnCancellation {
                    runCatching { context.unregisterReceiver(receiver) }
                }
                @Suppress("DEPRECATION")
                val started = wifiManager.startScan()
                if (!started) {
                    runCatching { context.unregisterReceiver(receiver) }
                    if (continuation.isActive) continuation.resume(Unit)
                }
            }
        }
    }

    override suspend fun connectToAp(ssid: String, password: String): Result<Unit> =
        suspendCancellableCoroutine { continuation ->
            val specifier = WifiNetworkSpecifier.Builder()
                .setSsid(ssid)
                .setWpa2Passphrase(password)
                .build()

            val request = NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .setNetworkSpecifier(specifier)
                .build()

            val callback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    boundNetwork = network
                    connectivityManager.bindProcessToNetwork(network)
                    if (continuation.isActive) continuation.resume(Result.success(Unit))
                }

                override fun onUnavailable() {
                    if (continuation.isActive) {
                        continuation.resume(Result.failure(IllegalStateException("wifi_connection_timeout")))
                    }
                }

                override fun onLost(network: Network) {
                    if (boundNetwork == network) {
                        connectivityManager.bindProcessToNetwork(null)
                        boundNetwork = null
                    }
                }
            }
            boundCallback = callback
            connectivityManager.requestNetwork(request, callback, CONNECT_TIMEOUT_MS)

            continuation.invokeOnCancellation {
                runCatching { connectivityManager.unregisterNetworkCallback(callback) }
            }
        }

    override fun releaseApBinding() {
        connectivityManager.bindProcessToNetwork(null)
        boundCallback?.let { runCatching { connectivityManager.unregisterNetworkCallback(it) } }
        boundCallback = null
        boundNetwork = null
    }

    override suspend fun awaitHomeWifiRestored(timeoutMs: Long): Boolean =
        withTimeoutOrNull(timeoutMs) {
            // bindProcessToNetwork(null) is synchronous, but the radio needs a brief moment to
            // reassociate with the home AP before HTTP calls to lan_ip will succeed. Checking only
            // `activeNetwork != null` is not enough: as soon as the process is unbound, Android is
            // free to route through mobile data while it's still reassociating with the home
            // Wi-Fi, and a verify call to the device's private LAN IP over mobile data will just
            // time out. Wait specifically for a Wi-Fi-transport active network.
            while (!isActiveNetworkWifi()) {
                delay(POLL_INTERVAL_MS)
            }
            true
        } ?: false

    private fun isActiveNetworkWifi(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    companion object {
        private const val CONNECT_TIMEOUT_MS = 30_000
        private const val POLL_INTERVAL_MS = 300L
        private const val SCAN_TIMEOUT_MS = 8_000L
        private const val MAX_FALLBACK_POLL_ATTEMPTS = 6
        private const val FALLBACK_POLL_INTERVAL_MS = 3_000L
    }
}
