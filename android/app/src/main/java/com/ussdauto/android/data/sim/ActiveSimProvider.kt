package com.ussdauto.android.data.sim

/** Petite abstraction au-dessus de SubscriptionManager, pour garder SimSlotManagerImpl
 *  testable en JVM pur (SubscriptionInfo est une classe Android non mockable simplement). */
interface ActiveSimProvider {
    fun activeSims(): List<ActiveSimInfo>
}

data class ActiveSimInfo(
    val slotIndex: Int,
    val subscriptionId: Int,
    val carrierName: String?
)
