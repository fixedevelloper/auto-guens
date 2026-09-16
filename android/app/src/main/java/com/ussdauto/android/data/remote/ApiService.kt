package com.ussdauto.android.data.remote

import com.ussdauto.android.data.remote.dto.SimSlotDto
import com.ussdauto.android.data.remote.dto.StatusCallbackDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface ApiService {

    @POST("api/transactions/{id}/status")
    suspend fun postStatus(
        @Path("id") transactionId: String,
        @Header("X-Device-Api-Key") deviceApiKey: String,
        @Body body: StatusCallbackDto
    ): Response<Unit>

    @GET("api/devices/{deviceId}/sim-slots")
    suspend fun getSimSlots(
        @Path("deviceId") deviceId: String,
        @Header("X-Device-Api-Key") deviceApiKey: String
    ): Response<List<SimSlotDto>>
}
