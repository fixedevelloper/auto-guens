package com.ussdauto.android.domain.model

data class SimSlotConfig(
    val slotIndex: Int,
    val operator: String,
    val phoneNumberOnSim: String?,
    val actif: Boolean
)

/** Écart détecté entre la config attendue (Room, synchronisée depuis l'API) et la SIM
 *  physiquement présente sur ce slot (SubscriptionManager). */
data class SimSlotMismatch(
    val slotIndex: Int,
    val expectedOperator: String,
    val actualCarrierName: String?
)
