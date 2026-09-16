package com.ussdauto.android.data.ussd

import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.telephony.TelephonyManager
import androidx.annotation.RequiresApi
import com.ussdauto.android.domain.ussd.UssdExecutionCallback
import com.ussdauto.android.domain.ussd.UssdExecutor
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject

/**
 * Composition d'un code USSD (aller-retour simple) ciblant une SIM précise via
 * TelephonyManager.createForSubscriptionId(subscriptionId) (API 26+). Le ciblage fiable
 * d'une SIM précise par ce mécanisme dépend cependant de la version d'Android et de
 * l'OEM — voir le point d'extension documenté sur l'interface UssdExecutor si ce n'est
 * pas fiable sur un appareil donné.
 */
class UssdExecutorImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : UssdExecutor {

    private val baseTelephonyManager: TelephonyManager
        get() = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

    @RequiresApi(Build.VERSION_CODES.O)
    override fun execute(ussdCode: String, subscriptionId: Int, callback: UssdExecutionCallback) {
        val mainHandler = Handler(Looper.getMainLooper())
        val telephonyManager = baseTelephonyManager.createForSubscriptionId(subscriptionId)

        telephonyManager.sendUssdRequest(
            ussdCode,
            object : TelephonyManager.UssdResponseCallback() {
                override fun onReceiveUssdResponse(telephonyManager: TelephonyManager, request: String, response: CharSequence) {
                    Timber.i("Réponse USSD reçue (subscriptionId=%d) pour %s: %s", subscriptionId, request, response)
                    callback.onUssdResult(response.toString())
                }

                override fun onReceiveUssdResponseFailed(telephonyManager: TelephonyManager, request: String, failureCode: Int) {
                    Timber.w("Échec d'exécution USSD (subscriptionId=%d) pour %s (code=%d)", subscriptionId, request, failureCode)
                    callback.onUssdFailed(failureCode)
                }
            },
            mainHandler
        )
    }
}
