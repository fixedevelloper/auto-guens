package com.ussdauto.android.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.ussdauto.android.R
import com.ussdauto.android.domain.model.FailureReason
import com.ussdauto.android.domain.model.LocalTransaction
import com.ussdauto.android.domain.model.PendingCommand
import com.ussdauto.android.domain.model.SmsParseResult
import com.ussdauto.android.domain.model.TransactionStatusLocal
import com.ussdauto.android.domain.repository.TransactionLocalRepository
import com.ussdauto.android.domain.sim.SimSlotManager
import com.ussdauto.android.domain.sms.PendingTransactionRegistry
import com.ussdauto.android.domain.status.StatusNotifier
import com.ussdauto.android.domain.ussd.UssdExecutionCallback
import com.ussdauto.android.domain.ussd.UssdExecutor
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Charge la transaction depuis Room (déjà insérée par UssdFirebaseMessagingService),
 * résout la SIM à utiliser, compose le code USSD reçu, attend le SMS de confirmation
 * (jusqu'à SMS_TIMEOUT_MS), et notifie l'API à chaque changement de statut.
 */
@AndroidEntryPoint
class UssdExecutionForegroundService : Service() {

    @Inject lateinit var transactionLocalRepository: TransactionLocalRepository
    @Inject lateinit var simSlotManager: SimSlotManager
    @Inject lateinit var ussdExecutor: UssdExecutor
    @Inject lateinit var registry: PendingTransactionRegistry
    @Inject lateinit var statusNotifier: StatusNotifier

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Default + serviceJob)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannelIfNeeded()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val transactionId = intent?.getStringExtra(EXTRA_TRANSACTION_ID)
        if (transactionId == null) {
            stopSelf(startId)
            return START_NOT_STICKY
        }

        startForeground(NOTIFICATION_ID, buildNotification())

        serviceScope.launch {
            val transaction = transactionLocalRepository.findById(transactionId)
            if (transaction == null) {
                Timber.w("Transaction locale introuvable pour %s, arrêt du service", transactionId)
                stopSelf(startId)
                return@launch
            }

            processTransaction(transaction)
            registry.clear(transactionId)
            stopSelf(startId)
        }

        return START_NOT_STICKY
    }

    private suspend fun processTransaction(transaction: LocalTransaction) {
        val subscriptionId = simSlotManager.subscriptionIdFor(transaction.simSlotIndex)
        if (subscriptionId == null) {
            Timber.w("Aucune SIM physique pour le slot %d (transaction %s)", transaction.simSlotIndex, transaction.transactionId)
            statusNotifier.notifyStatusChange(
                transaction.transactionId, TransactionStatusLocal.FAILED,
                failureReason = FailureReason.SIM_SLOT_UNAVAILABLE.name
            )
            return
        }

        statusNotifier.notifyStatusChange(transaction.transactionId, TransactionStatusLocal.EXECUTING)
        registry.start(transaction.toPendingCommand(), subscriptionId)

        val result = executeAndAwaitConfirmation(transaction, subscriptionId)
        reportResult(transaction.transactionId, result)
    }

    private suspend fun executeAndAwaitConfirmation(transaction: LocalTransaction, subscriptionId: Int): SmsParseResult {
        val dialOutcome = CompletableDeferred<DialOutcome>()

        ussdExecutor.execute(transaction.ussdCode, subscriptionId, object : UssdExecutionCallback {
            override fun onUssdResult(message: String) {
                dialOutcome.complete(DialOutcome.Dialed(message))
            }

            override fun onUssdFailed(failureCode: Int) {
                dialOutcome.complete(DialOutcome.Failed(failureCode))
            }
        })

        return when (val outcome = dialOutcome.await()) {
            is DialOutcome.Failed -> SmsParseResult.Failure(
                FailureReason.USSD_EXECUTION_FAILED,
                "USSD failureCode=${outcome.failureCode}"
            )

            is DialOutcome.Dialed -> registry.awaitResult(transaction.transactionId, SMS_TIMEOUT_MS)
                ?: SmsParseResult.Failure(FailureReason.NO_SMS_RECEIVED, "")
        }
    }

    private suspend fun reportResult(transactionId: String, result: SmsParseResult) {
        when (result) {
            is SmsParseResult.Success -> statusNotifier.notifyStatusChange(
                transactionId, TransactionStatusLocal.SUCCESS,
                rawSmsContent = result.rawContent,
                operatorReference = result.operatorReference
            )

            is SmsParseResult.Failure -> statusNotifier.notifyStatusChange(
                transactionId, TransactionStatusLocal.FAILED,
                rawSmsContent = result.rawContent.ifBlank { null },
                failureReason = result.reason.name
            )
        }
    }

    override fun onDestroy() {
        serviceJob.cancel()
        super.onDestroy()
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText("Exécution d'une transaction en cours...")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannelIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, "Exécution USSD", NotificationManager.IMPORTANCE_LOW)
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun LocalTransaction.toPendingCommand() = PendingCommand(
        transactionId = transactionId,
        operationType = operationType,
        phone = phone,
        operator = operator,
        amount = amount,
        senderName = senderName,
        countryCode = countryCode,
        description = description,
        simSlotIndex = simSlotIndex,
        ussdCode = ussdCode
    )

    private sealed class DialOutcome {
        data class Dialed(val message: String) : DialOutcome()
        data class Failed(val failureCode: Int) : DialOutcome()
    }

    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "ussd_execution"
        private const val NOTIFICATION_ID = 1001
        private const val SMS_TIMEOUT_MS = 60_000L
        private const val EXTRA_TRANSACTION_ID = "transactionId"

        fun buildIntent(context: Context, transactionId: String): Intent {
            return Intent(context, UssdExecutionForegroundService::class.java)
                .putExtra(EXTRA_TRANSACTION_ID, transactionId)
        }
    }
}
