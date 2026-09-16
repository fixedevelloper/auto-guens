package com.ussdauto.android.worker

/**
 * Extrait de StatusNotifierWorker pour rester testable en JVM pur : CoroutineWorker
 * dépend de WorkerParameters (classe Android non instanciable simplement hors
 * Robolectric/tests instrumentés), donc la décision de retry elle-même est isolée ici.
 */
object StatusNotifierRetryPolicy {
    const val MAX_ATTEMPTS = 3

    /** [runAttemptCount] est 0-indexé (0 = première tentative). */
    fun shouldRetry(runAttemptCount: Int): Boolean = runAttemptCount + 1 < MAX_ATTEMPTS
}
