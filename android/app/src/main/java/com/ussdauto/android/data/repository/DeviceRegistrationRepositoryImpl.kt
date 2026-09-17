package com.ussdauto.android.data.repository

import com.ussdauto.android.data.local.DeviceCredentialsStore
import com.ussdauto.android.data.remote.ApiService
import com.ussdauto.android.data.remote.dto.UpdateFcmTokenDto
import com.ussdauto.android.domain.repository.DeviceRegistrationRepository
import timber.log.Timber
import javax.inject.Inject

class DeviceRegistrationRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val deviceCredentialsStore: DeviceCredentialsStore
) : DeviceRegistrationRepository {

    override suspend fun registerFcmToken(token: String): Result<Unit> {
        val deviceId = deviceCredentialsStore.deviceId
            ?: return Result.failure(IllegalStateException("deviceId non configuré"))
        val apiKey = deviceCredentialsStore.apiKey
            ?: return Result.failure(IllegalStateException("apiKey non configurée"))

        return try {
            val response = apiService.updateFcmToken(
                deviceId = deviceId,
                deviceApiKey = apiKey,
                body = UpdateFcmTokenDto(fcmToken = token)
            )

            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Timber.w("Enregistrement du token FCM refusé par l'API (HTTP %d)", response.code())
                Result.failure(IllegalStateException("HTTP ${response.code()}"))
            }
        } catch (e: Exception) {
            Timber.w(e, "Échec réseau lors de l'enregistrement du token FCM")
            Result.failure(e)
        }
    }
}
