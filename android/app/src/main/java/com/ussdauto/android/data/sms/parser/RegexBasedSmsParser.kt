package com.ussdauto.android.data.sms.parser

import com.ussdauto.android.data.sms.config.SmsParserConfig
import com.ussdauto.android.domain.model.FailureReason
import com.ussdauto.android.domain.model.OperationType
import com.ussdauto.android.domain.model.SmsParseResult
import com.ussdauto.android.domain.sms.SmsParser
import java.math.BigDecimal

/**
 * Logique commune aux parseurs par opérateur : les regex et libellés diffèrent
 * (voir [config]), mais l'algorithme de rapprochement SMS <-> opération attendue
 * est le même pour tous les opérateurs.
 */
abstract class RegexBasedSmsParser(
    private val operateurKey: String,
    private val config: SmsParserConfig
) : SmsParser {

    override fun supports(sender: String): Boolean {
        val patterns = config.forOperateur(operateurKey) ?: return false
        return patterns.senders.any { sender.contains(it, ignoreCase = true) }
    }

    override fun parse(
        rawSms: String,
        expectedOperationType: OperationType,
        expectedAmount: BigDecimal
    ): SmsParseResult {
        val patterns = config.forOperateur(operateurKey)
            ?: return SmsParseResult.Failure(FailureReason.UNRECOGNIZED_FORMAT, rawSms)

        if (patterns.failurePattern?.containsMatchIn(rawSms) == true) {
            return SmsParseResult.Failure(FailureReason.OPERATOR_ERROR_SMS, rawSms)
        }

        val match = patterns.successPattern.find(rawSms)
            ?: return SmsParseResult.Failure(FailureReason.UNRECOGNIZED_FORMAT, rawSms)

        val operationKeyword = patterns.operationKeywords[expectedOperationType]
        val operationLabel = runCatching { match.groups["operation"]?.value }.getOrNull()
        val operationTypeMatches = when {
            operationKeyword != null && operationLabel != null -> operationKeyword.containsMatchIn(operationLabel)
            // À défaut de groupe "operation" explicite dans le SMS, on vérifie que le mot-clé
            // attendu apparaît quelque part dans le message (garde-fou moins précis mais sûr).
            operationKeyword != null -> operationKeyword.containsMatchIn(rawSms)
            else -> true
        }

        if (!operationTypeMatches) {
            return SmsParseResult.Failure(FailureReason.OPERATION_TYPE_MISMATCH, rawSms)
        }

        val reference = runCatching { match.groups["reference"]?.value }.getOrNull()
            ?: return SmsParseResult.Failure(FailureReason.UNRECOGNIZED_FORMAT, rawSms)

        val confirmedAmount = runCatching { match.groups["montant"]?.value }
            .getOrNull()
            ?.replace(",", ".")
            ?.let { runCatching { BigDecimal(it) }.getOrNull() }

        if (confirmedAmount != null && confirmedAmount.compareTo(expectedAmount) != 0) {
            return SmsParseResult.Failure(FailureReason.AMOUNT_MISMATCH, rawSms)
        }

        return SmsParseResult.Success(
            operatorReference = reference,
            confirmedAmount = confirmedAmount,
            rawContent = rawSms
        )
    }
}
