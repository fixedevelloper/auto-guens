package com.ussdauto.android.data.repository

import com.ussdauto.android.data.local.DeviceCredentialsStore
import com.ussdauto.android.data.remote.ApiService
import com.ussdauto.android.data.remote.dto.StatusCallbackDto
import com.ussdauto.android.domain.model.TransactionStatusLocal
import com.ussdauto.android.domain.repository.StatusCallbackRepository
import timber.log.Timber
import java.time.Instant
import javax.inject.Inject

class StatusCallbackRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val deviceCredentialsStore: DeviceCredentialsStore
) : StatusCallbackRepository {

    override suspend fun postStatus(
        transactionId: String,
        status: TransactionStatusLocal,
        timestamp: Instant,
        rawSmsContent: String?,
        operatorReference: String?,
        failureReason: String?
    ): Result<Unit> {
        val apiKey = deviceCredentialsStore.apiKey
            ?: return Result.failure(IllegalStateException("Aucune clé API device configurée"))

        return try {
            val response = apiService.postStatus(
                transactionId = transactionId,
                deviceApiKey = apiKey,
                body = StatusCallbackDto(
                    status = status.name,
                    timestamp = timestamp.toString(),
                    rawSmsContent = rawSmsContent,
                    operatorReference = operatorReference,
                    failureReason = failureReason
                )
            )

            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Timber.w("Callback statut refusé par l'API (HTTP %d) pour transaction %s", response.code(), transactionId)
                Result.failure(IllegalStateException("HTTP ${response.code()}"))
            }
        } catch (e: Exception) {
            Timber.w(e, "Échec réseau lors de l'envoi du statut pour transaction %s", transactionId)
            Result.failure(e)
        }
    }
}
