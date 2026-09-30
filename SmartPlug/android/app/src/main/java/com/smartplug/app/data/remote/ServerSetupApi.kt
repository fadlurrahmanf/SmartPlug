package com.smartplug.app.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.Response
import retrofit2.http.FieldMap
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST

/** AP-local ServerSmartPlug setup contract. Calls are made only while the app is bound to its AP. */
interface ServerSetupApi {
    @GET("/setup/status")
    suspend fun status(): Response<ServerSetupStatusDto>

    @FormUrlEncoded
    @POST("/setup")
    suspend fun apply(@FieldMap values: Map<String, String>): Response<Unit>
}

@JsonClass(generateAdapter = true)
data class ServerSetupStatusDto(
    @Json(name = "server_id") val serverId: String = "",
    @Json(name = "mdns_host") val mdnsHost: String = "",
    @Json(name = "mqtt_port") val mqttPort: Int = 1883,
    val station: ServerSetupStationDto = ServerSetupStationDto(),
)

@JsonClass(generateAdapter = true)
data class ServerSetupStationDto(
    val configured: Boolean = false,
    val connected: Boolean = false,
    val ip: String = "",
)
