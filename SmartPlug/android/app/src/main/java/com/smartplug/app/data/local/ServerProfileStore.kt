package com.smartplug.app.data.local

import com.smartplug.app.domain.model.RegisteredServer
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/** Small encrypted registry, separate from per-device Room data so a server may be added first. */
@Singleton
class ServerProfileStore @Inject constructor(
    private val tokenStore: SecureTokenStore,
) {
    fun all(): List<RegisteredServer> = runCatching {
        val items = JSONArray(tokenStore.serverProfilesJson())
        buildList {
            for (index in 0 until items.length()) {
                val value = items.getJSONObject(index)
                add(RegisteredServer(
                    serverId = value.getString("id"), displayName = value.getString("name"),
                    host = value.getString("host"), mqttPort = value.optInt("mqttPort", 1883),
                    mqttUsername = value.optString("mqttUsername", "SmartPlug"),
                    mqttPassword = value.optString("mqttPassword", "deviotsolution"),
                ))
            }
        }
    }.getOrDefault(emptyList())

    fun save(server: RegisteredServer, apiToken: String) {
        val profiles = all().filterNot { it.serverId == server.serverId }.toMutableList().apply { add(server) }
        val serialized = JSONArray().also { array -> profiles.forEach { profile ->
            array.put(JSONObject().apply {
                put("id", profile.serverId); put("name", profile.displayName); put("host", profile.host)
                put("mqttPort", profile.mqttPort); put("mqttUsername", profile.mqttUsername); put("mqttPassword", profile.mqttPassword)
            })
        }}
        tokenStore.setServerProfilesJson(serialized.toString())
        tokenStore.setServerApiToken(server.serverId, apiToken)
    }

    /** Removes only this phone's saved server profile and its local API token. */
    fun remove(serverId: String) {
        val serialized = JSONArray().also { array ->
            all().filterNot { it.serverId == serverId }.forEach { profile ->
                array.put(JSONObject().apply {
                    put("id", profile.serverId); put("name", profile.displayName); put("host", profile.host)
                    put("mqttPort", profile.mqttPort); put("mqttUsername", profile.mqttUsername); put("mqttPassword", profile.mqttPassword)
                })
            }
        }
        tokenStore.setServerProfilesJson(serialized.toString())
        tokenStore.clearServerApiToken(serverId)
    }

    fun apiToken(serverId: String): String? = tokenStore.serverApiToken(serverId)

}
