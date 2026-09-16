package com.ussdauto.android.data.remote.dto

/** Miroir de TransactionStatusCallbackRequest côté API (POST /transactions/{id}/status). */
data class StatusCallbackDto(
    val status: String,
    val timestamp: String,          // Instant.toString() (ISO-8601)
    val rawSmsContent: String?,
    val operatorReference: String?,
    val failureReason: String?
)
