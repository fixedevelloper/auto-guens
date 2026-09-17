package com.ussdauto.android.data.remote.dto

/** Miroir de UpdateDeviceFcmTokenRequest côté API (PATCH /api/devices/{id}). */
data class UpdateFcmTokenDto(
    val fcmToken: String
)
