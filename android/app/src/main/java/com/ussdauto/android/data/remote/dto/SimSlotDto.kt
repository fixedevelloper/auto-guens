package com.ussdauto.android.data.remote.dto

/** Miroir de SimSlotResponse côté API (GET /devices/{deviceId}/sim-slots). */
data class SimSlotDto(
    val id: String,
    val deviceId: String,
    val slotIndex: Int,
    val operator: String,
    val phoneNumberOnSim: String?,
    val actif: Boolean,
    val updatedAt: String
)
