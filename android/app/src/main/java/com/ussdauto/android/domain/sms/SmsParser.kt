package com.ussdauto.android.domain.sms

import com.ussdauto.android.domain.model.OperationType
import com.ussdauto.android.domain.model.SmsParseResult
import java.math.BigDecimal

/**
 * Interprète le SMS de confirmation d'un opérateur pour une opération mobile money.
 * Une implémentation par opérateur (voir data.sms.parser.MtnSmsParser / OrangeSmsParser)
 * car chaque opérateur a ses propres libellés et formats de référence.
 */
interface SmsParser {

    /** true si ce parseur sait traiter les SMS provenant de cet expéditeur. */
    fun supports(sender: String): Boolean

    /**
     * Analyse [rawSms] en sachant quelle opération était attendue ([expectedOperationType],
     * [expectedAmount]), afin de détecter un mismatch (ex: SMS de retrait reçu alors qu'un
     * dépôt était en cours) plutôt que de faire confiance aveuglément au contenu du SMS.
     */
    fun parse(
        rawSms: String,
        expectedOperationType: OperationType,
        expectedAmount: BigDecimal
    ): SmsParseResult
}
