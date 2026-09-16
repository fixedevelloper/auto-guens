package com.ussdauto.android.domain.sms

import com.ussdauto.android.domain.model.PendingCommand
import com.ussdauto.android.domain.model.SmsParseResult
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fait le pont entre UssdExecutionForegroundService (qui attend un résultat après avoir
 * composé le code USSD sur une SIM donnée) et SmsReceiver (qui reçoit le SMS de
 * confirmation de façon asynchrone). Un BroadcastReceiver ne peut pas porter lui-même
 * un délai d'attente de 60s sans risquer un ANR, donc l'attente est portée par le
 * service appelant via [awaitResult].
 */
@Singleton
class PendingTransactionRegistry @Inject constructor() {

    private data class ActiveCommand(val command: PendingCommand, val subscriptionId: Int)

    private val current = AtomicReference<ActiveCommand?>(null)
    private val results = MutableSharedFlow<Pair<String, SmsParseResult>>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    fun start(command: PendingCommand, subscriptionId: Int) {
        current.set(ActiveCommand(command, subscriptionId))
    }

    fun currentCommand(): PendingCommand? = current.get()?.command

    /** subscriptionId attendu pour le SMS de confirmation de la commande en cours, s'il y en a une. */
    fun currentSubscriptionId(): Int? = current.get()?.subscriptionId

    /** Appelé par SmsReceiver lorsqu'un SMS pertinent a été analysé. */
    suspend fun submit(transactionId: String, result: SmsParseResult) {
        results.emit(transactionId to result)
    }

    /** Attend jusqu'à [timeoutMillis] le résultat correspondant à [transactionId], ou null si timeout. */
    suspend fun awaitResult(transactionId: String, timeoutMillis: Long): SmsParseResult? {
        return withTimeoutOrNull(timeoutMillis) {
            results.first { (id, _) -> id == transactionId }.second
        }
    }

    fun clear(transactionId: String) {
        current.updateAndGet { if (it?.command?.transactionId == transactionId) null else it }
    }
}
