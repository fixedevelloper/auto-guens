package com.ussdauto.android.data.ussd

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.telephony.TelephonyManager
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
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
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            // Ne jamais laisser sendUssdRequest lever un SecurityException ici : ce code tourne
            // dans un service déclenché par FCM, sans utilisateur pour accorder la permission —
            // une exception non catchée planterait tout le process, pas seulement cette commande.
            Timber.e("CALL_PHONE non accordée : impossible d'exécuter le code USSD (subscriptionId=%d)", subscriptionId)
            callback.onUssdFailed(FAILURE_CODE_MISSING_PERMISSION)
            return
        }

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

    private companion object {
        /** En dehors de la plage des codes réels de TelephonyManager.UssdResponseCallback
         * (USSD_RETURN_FAILURE=0, USSD_ERROR_SERVICE_UNAVAIL=1), pour rester distinguable. */
        const val FAILURE_CODE_MISSING_PERMISSION = -1
    }
}
