package com.smartplug.app.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Keystore-backed storage for secrets that must never sit in plain SharedPreferences, log lines,
 * or Room: per-device `owner_token`, the ServerSmartPlug API token, and any home Wi-Fi password
 * the user has asked the app to remember (design.md "Password Wi-Fi rumah").
 *
 * [EncryptedSharedPreferences] wraps a hardware-backed [MasterKey] in the Android Keystore; the
 * app never handles raw key material itself.
 */
@Singleton
class SecureTokenStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "smartplug_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    fun ownerToken(deviceId: String): String? = prefs.getString(ownerTokenKey(deviceId), null)

    fun setOwnerToken(deviceId: String, token: String) {
        prefs.edit().putString(ownerTokenKey(deviceId), token).apply()
    }

    fun clearOwnerToken(deviceId: String) {
        prefs.edit().remove(ownerTokenKey(deviceId)).apply()
    }

    var serverApiToken: String?
        get() = prefs.getString(KEY_SERVER_API_TOKEN, null)
        set(value) {
            prefs.edit().putString(KEY_SERVER_API_TOKEN, value).apply()
        }

    fun savedWifiPassword(ssid: String): String? = prefs.getString(wifiKey(ssid), null)

    fun saveWifiPassword(ssid: String, password: String) {
        prefs.edit().putString(wifiKey(ssid), password).apply()
    }

    /** Used only after every registered SmartPlug has accepted a factory-reset command. */
    fun clearAll() {
        prefs.edit().clear().apply()
    }

    private fun ownerTokenKey(deviceId: String) = "owner_token_$deviceId"
    private fun wifiKey(ssid: String) = "wifi_pw_$ssid"

    companion object {
        private const val KEY_SERVER_API_TOKEN = "server_api_token"
    }
}
