package com.ussdauto.android.data.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.ussdauto.android.domain.sms.PendingTransactionRegistry
import com.ussdauto.android.domain.sms.SmsParser
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * N'agit que si une commande est en attente de confirmation (voir PendingTransactionRegistry) —
 * ignore silencieusement tout SMS reçu hors contexte d'une transaction en cours. Le timeout de
 * 60s "pas de SMS" n'est pas géré ici : un BroadcastReceiver doit rendre la main rapidement, donc
 * l'attente/timeout est portée par UssdExecutionForegroundService via registry.awaitResult().
 *
 * Multi-SIM : l'extra "subscription" (posé par le framework Android sur l'intent SMS_RECEIVED
 * sur les appareils dual-SIM, bien que non formellement documenté dans l'API publique) permet
 * de vérifier que le SMS arrive bien sur la SIM utilisée pour la transaction en cours. S'il est
 * absent (-1, appareil mono-SIM ou OEM qui ne le renseigne pas), on ne filtre pas dessus.
 */
@AndroidEntryPoint
class SmsReceiver : BroadcastReceiver() {

    @Inject
    lateinit var parsers: Set<@JvmSuppressWildcards SmsParser>

    @Inject
    lateinit var registry: PendingTransactionRegistry

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val pending = registry.currentCommand() ?: run {
            Timber.d("SMS reçu sans transaction en attente, ignoré")
            return
        }

        val expectedSubscriptionId = registry.currentSubscriptionId()
        val incomingSubscriptionId = intent.getIntExtra("subscription", -1)
        if (expectedSubscriptionId != null && incomingSubscriptionId != -1 && incomingSubscriptionId != expectedSubscriptionId) {
            Timber.d("SMS reçu sur une autre SIM (subscription=%d, attendu=%d), ignoré",
                incomingSubscriptionId, expectedSubscriptionId)
            return
        }

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        val sender = messages.firstOrNull()?.originatingAddress
        val body = messages.joinToString(separator = "") { it.messageBody.orEmpty() }

        if (sender == null || body.isBlank()) return

        val parser = parsers.firstOrNull { it.supports(sender) } ?: run {
            Timber.d("Expéditeur SMS non reconnu: %s", sender)
            return
        }

        val result = parser.parse(body, pending.operationType, pending.amount)
        Timber.i("SMS analysé pour transaction %s: %s", pending.transactionId, result)

        val asyncResult = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                registry.submit(pending.transactionId, result)
            } finally {
                asyncResult.finish()
            }
        }
    }
}
