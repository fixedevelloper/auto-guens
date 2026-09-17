package com.ussdauto.android.domain.repository

/** Enregistre le token FCM courant de ce device auprès de l'API (PATCH /api/devices/{id}). */
interface DeviceRegistrationRepository {

    suspend fun registerFcmToken(token: String): Result<Unit>
}
