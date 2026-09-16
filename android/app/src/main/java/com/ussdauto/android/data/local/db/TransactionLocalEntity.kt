package com.ussdauto.android.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transaction_local")
data class TransactionLocalEntity(
    @PrimaryKey val id: String,              // = transactionId reçu de Spring
    val operationType: String,
    val phone: String,
    val operator: String,
    val amount: String,                      // BigDecimal stocké en texte (précision exacte)
    val senderName: String,
    val countryCode: String,
    val description: String?,
    val simSlotIndex: Int,
    val ussdCode: String,
    val statutLocal: String,
    val smsContentBrut: String?,
    val createdAt: Long,                     // epoch millis
    val lastStatusSentAt: Long?
)
