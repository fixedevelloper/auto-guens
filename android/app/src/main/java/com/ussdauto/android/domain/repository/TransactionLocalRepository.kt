package com.ussdauto.android.domain.repository

import com.ussdauto.android.domain.model.LocalTransaction
import com.ussdauto.android.domain.model.PendingCommand
import com.ussdauto.android.domain.model.TransactionStatusLocal
import kotlinx.coroutines.flow.Flow
import java.time.Instant

interface TransactionLocalRepository {

    /**
     * Insère la transaction si elle n'existe pas déjà (clé = transactionId reçu de
     * Spring). Retourne false si elle existait déjà (livraison FCM dupliquée) — dans
     * ce cas l'appelant ne doit pas relancer l'exécution.
     */
    suspend fun insertIfNew(command: PendingCommand): Boolean

    suspend fun updateStatus(
        transactionId: String,
        status: TransactionStatusLocal,
        smsContentBrut: String? = null
    )

    suspend fun markStatusSent(transactionId: String, sentAt: Instant)

    suspend fun findById(transactionId: String): LocalTransaction?

    /** Historique complet des transactions (les plus récentes en premier), pour l'écran de statut. */
    fun observeAll(): Flow<List<LocalTransaction>>
}
