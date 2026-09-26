package com.smartplug.app.data.remote

import com.smartplug.app.data.remote.dto.ServerCommandStatusDto
import com.smartplug.app.data.remote.dto.ServerDeviceDto
import com.smartplug.app.data.remote.dto.ServerEnergyDto
import com.smartplug.app.data.remote.dto.ServerHistoryResponseDto
import com.smartplug.app.data.remote.dto.ServerLatestDto
import com.smartplug.app.data.remote.dto.ServerRelayResponseDto
import com.smartplug.app.data.remote.dto.DeviceScheduleDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * ServerSmartPlug's application REST API (design.md "REST API aplikasi ke server"). Bound to the
 * discovered `srvrplug-<server_id>.local` host. All calls carry the app's API token.
 */
interface ServerApi {

    @GET("/api/v1/status")
    suspend fun getServerStatus(
        @Header("Authorization") bearerToken: String,
    ): Response<Unit>

    @GET("/api/v1/devices")
    suspend fun listDevices(
        @Header("Authorization") bearerToken: String,
    ): Response<List<ServerDeviceDto>>

    @GET("/api/v1/devices/{device_id}/latest")
    suspend fun getLatest(
        @Header("Authorization") bearerToken: String,
        @Path("device_id") deviceId: String,
    ): Response<ServerLatestDto>

    @GET("/api/v1/devices/{device_id}/energy")
    suspend fun getEnergy(
        @Header("Authorization") bearerToken: String,
        @Path("device_id") deviceId: String,
    ): Response<ServerEnergyDto>

    @GET("/api/v1/devices/{device_id}/history")
    suspend fun getHistory(
        @Header("Authorization") bearerToken: String,
        @Path("device_id") deviceId: String,
        @Query("from") fromUtc: String,
        @Query("to") toUtc: String,
        @Query("resolution") resolution: String,
    ): Response<ServerHistoryResponseDto>

    @POST("/api/v1/devices/{device_id}/relay")
    suspend fun setRelay(
        @Header("Authorization") bearerToken: String,
        @Path("device_id") deviceId: String,
        @Body request: Map<String, String>,
    ): Response<ServerRelayResponseDto>

    @POST("/api/v1/devices/{device_id}/energy/reset")
    suspend fun resetEnergy(
        @Header("Authorization") bearerToken: String,
        @Path("device_id") deviceId: String,
        @Body confirmations: Map<String, String>,
    ): Response<Unit>

    @POST("/api/v1/devices/{device_id}/factory-reset")
    suspend fun factoryReset(
        @Header("Authorization") bearerToken: String,
        @Path("device_id") deviceId: String,
        @Body confirmations: Map<String, String>,
    ): Response<Unit>

    @POST("/api/v1/devices/{device_id}/timer")
    suspend fun setTimer(
        @Header("Authorization") bearerToken: String,
        @Path("device_id") deviceId: String,
        @Body values: Map<String, Any>,
    ): Response<Unit>

    @GET("/api/v1/devices/{device_id}/schedule")
    suspend fun getSchedule(
        @Header("Authorization") bearerToken: String,
        @Path("device_id") deviceId: String,
    ): Response<DeviceScheduleDto>

    @POST("/api/v1/devices/{device_id}/schedule")
    suspend fun setSchedule(
        @Header("Authorization") bearerToken: String,
        @Path("device_id") deviceId: String,
        @Body values: Map<String, Any>,
    ): Response<DeviceScheduleDto>

    @GET("/api/v1/commands/{command_id}")
    suspend fun getCommandStatus(
        @Header("Authorization") bearerToken: String,
        @Path("command_id") commandId: String,
    ): Response<ServerCommandStatusDto>
}
