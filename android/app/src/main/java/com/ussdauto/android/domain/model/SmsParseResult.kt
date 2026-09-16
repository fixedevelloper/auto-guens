package com.ussdauto.android.domain.model

import java.math.BigDecimal

sealed class SmsParseResult {

    data class Success(
        val operatorReference: String,
        val confirmedAmount: BigDecimal?,
        val rawContent: String
    ) : SmsParseResult()

    data class Failure(
        val reason: FailureReason,
        val rawContent: String
    ) : SmsParseResult()
}

enum class FailureReason {
    /** Le SMS ne correspond à aucun format de regex connu pour cet opérateur. */
    UNRECOGNIZED_FORMAT,

    /** Le SMS confirme une opération, mais pas celle attendue (ex: reçu "retrait" alors qu'un DEPOSIT était en cours). */
    OPERATION_TYPE_MISMATCH,

    /** Le SMS confirme un montant différent de celui de la transaction. */
    AMOUNT_MISMATCH,

    /** Le SMS indique explicitement un échec de l'opération côté opérateur (solde insuffisant, etc.). */
    OPERATOR_ERROR_SMS,

    /** Aucun SMS de confirmation reçu dans le délai imparti. */
    NO_SMS_RECEIVED,

    /** Le composeur USSD lui-même a échoué (réseau, code invalide, timeout du menu). */
    USSD_EXECUTION_FAILED,

    /** Le slotIndex logique attendu ne correspond à aucune SIM physique actuellement détectée. */
    SIM_SLOT_UNAVAILABLE
}
