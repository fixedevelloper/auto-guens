package com.ussdauto.android.data.status

import android.content.Context
import com.ussdauto.android.domain.model.TransactionStatusLocal
import com.ussdauto.android.domain.repository.TransactionLocalRepository
import com.ussdauto.android.domain.status.StatusNotifier
import com.ussdauto.android.worker.StatusNotifierWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import javax.inject.Inject

class StatusNotifierImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val transactionLocalRepository: TransactionLocalRepository
) : StatusNotifier {

    override suspend fun notifyStatusChange(
        transactionId: String,
        status: TransactionStatusLocal,
        timestamp: Instant,
        rawSmsContent: String?,
        operatorReference: String?,
        failureReason: String?
    ) {
        transactionLocalRepository.updateStatus(transactionId, status, rawSmsContent)

        StatusNotifierWorker.enqueue(
            context = context,
            transactionId = transactionId,
            status = status,
            timestamp = timestamp,
            rawSmsContent = rawSmsContent,
            operatorReference = operatorReference,
            failureReason = failureReason
        )
    }
}
