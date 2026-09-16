package com.ussdauto.android.domain.sim

import com.ussdauto.android.domain.model.SimSlotConfig
import com.ussdauto.android.domain.model.SimSlotMismatch

interface SimSlotManager {

    /** GET /devices/{deviceId}/sim-slots, persiste en Room. Appelé au démarrage de
     *  l'app et sur réception FCM SIM_CONFIG_UPDATED. */
    suspend fun syncFromRemote(): Result<Unit>

    /** Config actuellement connue localement (source pour l'UI en lecture seule). */
    suspend fun currentConfig(): List<SimSlotConfig>

    /** subscriptionId Android (SubscriptionManager) correspondant à un slotIndex
     *  logique, pour que UssdExecutor cible la bonne puce. Null si aucune SIM
     *  physique ne correspond actuellement à ce slot. */
    fun subscriptionIdFor(slotIndex: Int): Int?

    /** Compare la config attendue (Room) aux SIM physiquement présentes
     *  (SubscriptionManager.getActiveSubscriptionInfoList()). */
    suspend fun validatePhysicalSims(): List<SimSlotMismatch>
}
