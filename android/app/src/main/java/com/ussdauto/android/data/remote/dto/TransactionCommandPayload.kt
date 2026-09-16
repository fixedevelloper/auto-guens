package com.ussdauto.android.data.remote.dto

import com.ussdauto.android.domain.model.OperationType
import com.ussdauto.android.domain.model.PendingCommand
import timber.log.Timber
import java.math.BigDecimal

/**
 * Payload data-only FCM d'une commande de transaction. FCM livre toujours des
 * String -> String, d'où ce parsing manuel plutôt qu'une désérialisation Gson.
 */
data class TransactionCommandPayload(
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
) {
    fun toPendingCommand() = PendingCommand(
        transactionId = transactionId,
        operationType = operationType,
        phone = phone,
        operator = operator,
        amount = amount,
        senderName = senderName,
        countryCode = countryCode,
        description = description,
        simSlotIndex = simSlotIndex,
        ussdCode = ussdCode
    )

    companion object {
        const val MESSAGE_TYPE_KEY = "messageType"
        const val MESSAGE_TYPE_COMMAND = "TRANSACTION_COMMAND"
        const val MESSAGE_TYPE_SIM_CONFIG_UPDATED = "SIM_CONFIG_UPDATED"

        fun fromData(data: Map<String, String>): TransactionCommandPayload? {
            return try {
                TransactionCommandPayload(
                    transactionId = data.getValue("transactionId"),
                    operationType = OperationType.valueOf(data.getValue("operationType")),
                    phone = data.getValue("phone"),
                    operator = data.getValue("operator"),
                    amount = BigDecimal(data.getValue("amount")),
                    senderName = data.getValue("senderName"),
                    countryCode = data.getValue("countryCode"),
                    description = data["description"]?.takeIf { it.isNotBlank() },
                    simSlotIndex = data.getValue("simSlotIndex").toInt(),
                    ussdCode = data.getValue("ussdCode")
                )
            } catch (e: Exception) {
                Timber.e(e, "Payload FCM de commande invalide ou incomplet: %s", data)
                null
            }
        }
    }
}
