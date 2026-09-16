package com.ussdauto.android.domain.model

/** Sous-ensemble de com.ussdauto.api.domain.enums.TransactionStatus que le device est
 *  autorisé à rapporter via POST /transactions/{id}/status. */
enum class TransactionStatusLocal {
    RECEIVED_BY_DEVICE,
    EXECUTING,
    SUCCESS,
    FAILED
}
