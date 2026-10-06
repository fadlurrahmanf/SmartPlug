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
 * or Room: per-device access credentials (owner or member), the ServerSmartPlug API token, and any home Wi-Fi password
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

    /**
     * Returns this phone's credential for one device.  It may be the original owner token or a
     * separately issued member credential; callers must never infer the role from this value.
     */
    fun accessCredential(deviceId: String): String? = prefs.getString(accessCredentialKey(deviceId), null)

    /**
     * Stores an access credential whose role is not known locally.  This only
     * exists for migration/legacy callers; server-configuration code must
     * resolve it before treating it as an owner credential.
     */
    fun setAccessCredential(deviceId: String, credential: String) {
        prefs.edit().putString(accessCredentialKey(deviceId), credential).apply()
    }

    /** Credentials issued by the additional-phone invitation flow are always members. */
    fun setMemberCredential(deviceId: String, credential: String) {
        prefs.edit()
            .putString(accessCredentialKey(deviceId), credential)
            .putString(accessRoleKey(deviceId), DeviceAccessRole.MEMBER.storageValue)
            .apply()
    }

    fun accessRole(deviceId: String): DeviceAccessRole = when (
        prefs.getString(accessRoleKey(deviceId), null)
    ) {
        DeviceAccessRole.OWNER.storageValue -> DeviceAccessRole.OWNER
        DeviceAccessRole.MEMBER.storageValue -> DeviceAccessRole.MEMBER
        else -> DeviceAccessRole.UNKNOWN
    }

    fun clearAccessCredential(deviceId: String) {
        prefs.edit()
            .remove(accessCredentialKey(deviceId))
            .remove(accessRoleKey(deviceId))
            .apply()
    }

    /** Backward-compatible names for the original Direct onboarding path. */
    fun ownerToken(deviceId: String): String? = accessCredential(deviceId)

    fun setOwnerToken(deviceId: String, token: String) {
        prefs.edit()
            .putString(accessCredentialKey(deviceId), token)
            .putString(accessRoleKey(deviceId), DeviceAccessRole.OWNER.storageValue)
            .apply()
    }

    fun clearOwnerToken(deviceId: String) {
        clearAccessCredential(deviceId)
    }

    var serverApiToken: String?
        get() = prefs.getString(KEY_SERVER_API_TOKEN, null)
        set(value) {
            prefs.edit().putString(KEY_SERVER_API_TOKEN, value).apply()
        }

    fun serverApiToken(serverId: String): String? = prefs.getString(serverTokenKey(serverId), null)

    fun setServerApiToken(serverId: String, token: String) {
        prefs.edit().putString(serverTokenKey(serverId), token).apply()
    }

    fun clearServerApiToken(serverId: String) {
        prefs.edit().remove(serverTokenKey(serverId)).apply()
    }

    fun serverProfilesJson(): String = prefs.getString(KEY_SERVER_PROFILES, "[]") ?: "[]"

    fun setServerProfilesJson(value: String) {
        prefs.edit().putString(KEY_SERVER_PROFILES, value).apply()
    }

    fun savedWifiPassword(ssid: String): String? = prefs.getString(wifiKey(ssid), null)

    fun saveWifiPassword(ssid: String, password: String) {
        prefs.edit().putString(wifiKey(ssid), password).apply()
    }

    /** Used only after every registered SmartPlug has accepted a factory-reset command. */
    fun clearAll() {
        prefs.edit().clear().apply()
    }

    // Keep the existing encrypted preference key so upgrading the app does not discard an
    // already-working Direct owner credential.  The semantic API above is role-neutral.
    private fun accessCredentialKey(deviceId: String) = "owner_token_$deviceId"
    private fun accessRoleKey(deviceId: String) = "access_role_$deviceId"
    private fun wifiKey(ssid: String) = "wifi_pw_$ssid"
    private fun serverTokenKey(serverId: String) = "server_api_token_$serverId"

    companion object {
        private const val KEY_SERVER_API_TOKEN = "server_api_token"
        private const val KEY_SERVER_PROFILES = "server_profiles"
    }
}

/** Role metadata is encrypted alongside the credential, never stored in Room. */
enum class DeviceAccessRole(internal val storageValue: String) {
    OWNER("owner"),
    MEMBER("member"),
    UNKNOWN("unknown"),
}
