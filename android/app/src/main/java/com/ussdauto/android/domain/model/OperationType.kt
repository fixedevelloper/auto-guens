package com.ussdauto.android.domain.model

/**
 * Reflète com.ussdauto.api.domain.enums.OperationType côté API. Le type d'opération
 * détermine le libellé de SMS attendu (dépôt vs retrait) mais jamais le code USSD à
 * composer : celui-ci est toujours résolu côté API et transmis déjà prêt à l'app.
 */
enum class OperationType {
    DEPOSIT,
    WITHDRAW
}
