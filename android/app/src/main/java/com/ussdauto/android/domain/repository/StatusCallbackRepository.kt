package com.ussdauto.android.domain.repository

import com.ussdauto.android.domain.model.TransactionStatusLocal
import java.time.Instant

/** Une seule tentative d'appel réseau — le retry est géré par StatusNotifierWorker (WorkManager). */
interface StatusCallbackRepository {

    suspend fun postStatus(
        transactionId: String,
        status: TransactionStatusLocal,
        timestamp: Instant,
        rawSmsContent: String?,
        operatorReference: String?,
        failureReason: String?
    ): Result<Unit>
}
