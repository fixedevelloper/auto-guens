package com.ussdauto.android.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.ussdauto.android.domain.model.TransactionStatusLocal
import com.ussdauto.android.domain.repository.StatusCallbackRepository
import com.ussdauto.android.domain.repository.TransactionLocalRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber
import java.time.Instant
import java.util.concurrent.TimeUnit

/**
 * Envoie un changement de statut à l'API. 3 tentatives au total, backoff exponentiel
 * géré nativement par WorkManager. Marque lastStatusSentAt en Room une fois confirmé,
 * pour éviter les doublons visuels en cas de redémarrage de l'app (le callback API
 * reste de toute façon idempotent côté serveur).
 */
@HiltWorker
class StatusNotifierWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val statusCallbackRepository: StatusCallbackRepository,
    private val transactionLocalRepository: TransactionLocalRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val transactionId = inputData.getString(KEY_TRANSACTION_ID) ?: return Result.failure()
        val status = inputData.getString(KEY_STATUS)?.let {
            runCatching { TransactionStatusLocal.valueOf(it) }.getOrNull()
        } ?: return Result.failure()
        val timestamp = inputData.getString(KEY_TIMESTAMP)?.let {
            runCatching { Instant.parse(it) }.getOrNull()
        } ?: Instant.now()

        val outcome = statusCallbackRepository.postStatus(
            transactionId = transactionId,
            status = status,
            timestamp = timestamp,
            rawSmsContent = inputData.getString(KEY_RAW_SMS_CONTENT),
            operatorReference = inputData.getString(KEY_OPERATOR_REFERENCE),
            failureReason = inputData.getString(KEY_FAILURE_REASON)
        )

        if (outcome.isSuccess) {
            transactionLocalRepository.markStatusSent(transactionId, Instant.now())
            return Result.success()
        }

        return if (StatusNotifierRetryPolicy.shouldRetry(runAttemptCount)) {
            Result.retry()
        } else {
            Timber.e("Abandon définitif de l'envoi du statut %s pour %s après %d tentatives",
                status, transactionId, runAttemptCount + 1)
            Result.failure()
        }
    }

    companion object {
        private const val KEY_TRANSACTION_ID = "transactionId"
        private const val KEY_STATUS = "status"
        private const val KEY_TIMESTAMP = "timestamp"
        private const val KEY_RAW_SMS_CONTENT = "rawSmsContent"
        private const val KEY_OPERATOR_REFERENCE = "operatorReference"
        private const val KEY_FAILURE_REASON = "failureReason"

        fun enqueue(
            context: Context,
            transactionId: String,
            status: TransactionStatusLocal,
            timestamp: Instant,
            rawSmsContent: String?,
            operatorReference: String?,
            failureReason: String?
        ) {
            val data = workDataOf(
                KEY_TRANSACTION_ID to transactionId,
                KEY_STATUS to status.name,
                KEY_TIMESTAMP to timestamp.toString(),
                KEY_RAW_SMS_CONTENT to rawSmsContent,
                KEY_OPERATOR_REFERENCE to operatorReference,
                KEY_FAILURE_REASON to failureReason
            )

            val request = OneTimeWorkRequestBuilder<StatusNotifierWorker>()
                .setInputData(data)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, WorkRequest.MIN_BACKOFF_MILLIS, TimeUnit.MILLISECONDS)
                .build()

            WorkManager.getInstance(context).enqueue(request)
        }
    }
}
