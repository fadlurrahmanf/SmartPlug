package com.smartplug.app.data.remote

import com.smartplug.app.data.remote.dto.AllParametersResponseDto
import com.smartplug.app.data.remote.dto.DeviceStatusDto
import com.smartplug.app.data.remote.dto.DeviceScheduleDto
import com.smartplug.app.data.remote.dto.RelayCommandRequestDto
import com.smartplug.app.data.remote.dto.RelayCommandResponseDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.FieldMap
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * SmartPlug's operational REST API used once the device has an owner token (design.md
 * "REST operasional"). Bound to whatever `lan_ip` onboarding/mDNS discovery last resolved.
 */
interface DeviceApi {

    @GET("/api/v1/status")
    suspend fun getStatus(
        @Header("Authorization") bearerToken: String,
    ): Response<DeviceStatusDto>

    @GET("/api/v1/measurements/allparameters")
    suspend fun getAllParameters(
        @Header("Authorization") bearerToken: String,
    ): Response<AllParametersResponseDto>

    @POST("/api/v1/relay")
    suspend fun setRelay(
        @Header("Authorization") bearerToken: String,
        @Body request: RelayCommandRequestDto,
    ): Response<RelayCommandResponseDto>

    @FormUrlEncoded
    @POST("/api/v1/energy/reset")
    suspend fun resetEnergy(
        @Header("Authorization") bearerToken: String,
        @FieldMap confirmations: Map<String, String>,
    ): Response<Unit>

    @FormUrlEncoded
    @POST("/api/v1/factory-reset")
    suspend fun factoryReset(
        @Header("Authorization") bearerToken: String,
        @FieldMap confirmations: Map<String, String>,
    ): Response<Unit>

    @FormUrlEncoded
    @POST("/api/v1/timer")
    suspend fun setTimer(
        @Header("Authorization") bearerToken: String,
        @FieldMap values: Map<String, String>,
    ): Response<Unit>

    @GET("/api/v1/schedule")
    suspend fun getSchedule(
        @Header("Authorization") bearerToken: String,
    ): Response<DeviceScheduleDto>

    @FormUrlEncoded
    @POST("/api/v1/schedule")
    suspend fun setSchedule(
        @Header("Authorization") bearerToken: String,
        @FieldMap values: Map<String, String>,
    ): Response<DeviceScheduleDto>
}
