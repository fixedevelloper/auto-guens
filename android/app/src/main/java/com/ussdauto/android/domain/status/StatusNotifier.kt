package com.ussdauto.android.domain.status

import com.ussdauto.android.domain.model.TransactionStatusLocal
import java.time.Instant

interface StatusNotifier {

    /**
     * Enregistre (de façon synchrone/suspend) la transition dans TransactionLocal
     * (Room), puis programme l'envoi vers l'API via WorkManager — l'envoi réseau
     * lui-même est fire-and-forget du point de vue de l'appelant, mais l'écriture
     * Room est attendue pour garantir l'ordre des transitions successives.
     * Appelé à CHAQUE changement de statut, pas seulement à la fin.
     */
    suspend fun notifyStatusChange(
        transactionId: String,
        status: TransactionStatusLocal,
        timestamp: Instant = Instant.now(),
        rawSmsContent: String? = null,
        operatorReference: String? = null,
        failureReason: String? = null
    )
}
