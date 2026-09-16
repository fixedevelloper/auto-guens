package com.ussdauto.android.data.sim

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.telephony.SubscriptionManager
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject

/**
 * READ_PHONE_STATE est déclarée dans le manifest et demandée au runtime au lancement
 * de StatusActivity (écran de provisioning de cet appareil dédié).
 */
class AndroidActiveSimProvider @Inject constructor(
    @ApplicationContext private val context: Context
) : ActiveSimProvider {

    override fun activeSims(): List<ActiveSimInfo> {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Timber.w("READ_PHONE_STATE non accordée : les SIM actives ne peuvent pas être listées")
            return emptyList()
        }

        val subscriptionManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
        return subscriptionManager.activeSubscriptionInfoList.orEmpty().map {
            ActiveSimInfo(
                slotIndex = it.simSlotIndex,
                subscriptionId = it.subscriptionId,
                carrierName = it.carrierName?.toString()
            )
        }
    }
}
