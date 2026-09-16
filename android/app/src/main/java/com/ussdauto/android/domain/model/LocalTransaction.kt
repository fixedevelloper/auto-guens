package com.ussdauto.android.domain.model

import java.math.BigDecimal
import java.time.Instant

/** Vue domaine de la ligne Room TransactionLocalEntity — source de vérité locale
 *  pendant l'exécution, indépendamment de la connectivité réseau. */
data class LocalTransaction(
    val transactionId: String,
    val operationType: OperationType,
    val phone: String,
    val operator: String,
    val amount: BigDecimal,
    val senderName: String,
    val countryCode: String,
    val description: String?,
    val simSlotIndex: Int,
    val ussdCode: String,
    val statutLocal: TransactionStatusLocal,
    val smsContentBrut: String?,
    val createdAt: Instant,
    val lastStatusSentAt: Instant?
)
