package com.ussdauto.android.domain.model

import java.math.BigDecimal

/**
 * Commande reçue via FCM. L'app ne fait qu'exécuter [ussdCode] et cibler [simSlotIndex]
 * tels quels — elle ne construit jamais elle-même un code USSD ni ne choisit l'opérateur.
 */
data class PendingCommand(
    val transactionId: String,
    val operationType: OperationType,
    val phone: String,
    val operator: String,
    val amount: BigDecimal,
    val senderName: String,
    val countryCode: String,
    val description: String?,
    val simSlotIndex: Int,
    val ussdCode: String
)
