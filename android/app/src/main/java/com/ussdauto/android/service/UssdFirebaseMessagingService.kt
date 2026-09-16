package com.ussdauto.android.service

import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.ussdauto.android.data.remote.dto.TransactionCommandPayload
import com.ussdauto.android.data.remote.dto.TransactionCommandPayload.Companion.MESSAGE_TYPE_COMMAND
import com.ussdauto.android.data.remote.dto.TransactionCommandPayload.Companion.MESSAGE_TYPE_KEY
import com.ussdauto.android.data.remote.dto.TransactionCommandPayload.Companion.MESSAGE_TYPE_SIM_CONFIG_UPDATED
import com.ussdauto.android.domain.model.TransactionStatusLocal
import com.ussdauto.android.domain.repository.TransactionLocalRepository
import com.ussdauto.android.domain.sim.SimSlotManager
import com.ussdauto.android.domain.status.StatusNotifier
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Deux types de messages, distingués par le champ data "messageType" (les deux sont des
 * messages data-only ciblés sur le token de ce device, pas un topic FCM partagé) :
 * - TRANSACTION_COMMAND : nouvelle commande à exécuter.
 * - SIM_CONFIG_UPDATED : la config des puces SIM a changé côté API, à resynchroniser.
 */
@AndroidEntryPoint
class UssdFirebaseMessagingService : FirebaseMessagingService() {

    @Inject lateinit var transactionLocalRepository: TransactionLocalRepository
    @Inject lateinit var statusNotifier: StatusNotifier
    @Inject lateinit var simSlotManager: SimSlotManager

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        when (remoteMessage.data[MESSAGE_TYPE_KEY]) {
            MESSAGE_TYPE_COMMAND -> handleTransactionCommand(remoteMessage.data)
            MESSAGE_TYPE_SIM_CONFIG_UPDATED -> handleSimConfigUpdated()
            else -> Timber.w("Message FCM ignoré: messageType inconnu %s", remoteMessage.data)
        }
    }

    private fun handleTransactionCommand(data: Map<String, String>) {
        val payload = TransactionCommandPayload.fromData(data)
        if (payload == null) {
            Timber.w("Commande FCM ignorée: payload invalide %s", data)
            return
        }

        serviceScope.launch {
            val isNew = transactionLocalRepository.insertIfNew(payload.toPendingCommand())
            if (!isNew) {
                Timber.i("Commande dupliquée ignorée pour transaction %s (déjà en base locale)", payload.transactionId)
                return@launch
            }

            Timber.i("Commande reçue: transaction=%s type=%s", payload.transactionId, payload.operationType)

            // Notifie Spring AVANT même d'exécuter l'USSD, pour confirmer la bonne réception.
            statusNotifier.notifyStatusChange(payload.transactionId, TransactionStatusLocal.RECEIVED_BY_DEVICE)

            ContextCompat.startForegroundService(
                this@UssdFirebaseMessagingService,
                UssdExecutionForegroundService.buildIntent(this@UssdFirebaseMessagingService, payload.transactionId)
            )
        }
    }

    private fun handleSimConfigUpdated() {
        serviceScope.launch {
            val result = simSlotManager.syncFromRemote()
            if (result.isFailure) {
                Timber.w(result.exceptionOrNull(), "Échec de resynchronisation de la config SIM suite à SIM_CONFIG_UPDATED")
            } else {
                Timber.i("Config SIM resynchronisée suite à SIM_CONFIG_UPDATED")
            }
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Timber.i("Nouveau token FCM généré")
        // Point d'extension : transmettre ce token à l'API pour mettre à jour Device.fcmToken.
        // Aucun endpoint de (ré)enregistrement de device n'est défini dans le périmètre actuel
        // de l'API — le token est aujourd'hui provisionné manuellement en base au setup du device.
    }
}
