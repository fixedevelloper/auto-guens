package com.ussdauto.android.domain.ussd

/**
 * Exécute un code USSD déjà résolu par l'API, sur la SIM identifiée par
 * [subscriptionId] (résolu par SimSlotManager à partir du simSlotIndex logique de la
 * commande). L'app ne décide jamais elle-même du code à composer ni de l'opérateur.
 */
interface UssdExecutor {

    fun execute(ussdCode: String, subscriptionId: Int, callback: UssdExecutionCallback)

    // Point d'extension : TelephonyManager.createForSubscriptionId(subscriptionId)
    // .sendUssdRequest() ne gère qu'un aller-retour simple, et son ciblage fiable d'une
    // SIM précise dépend de la version d'Android et de l'OEM (voir UssdExecutorImpl).
    // Si un opérateur exige un menu USSD à plusieurs étapes (ex : sous-menu DEPOSIT/
    // WITHDRAW, saisie d'un code PIN marchand), ou si sendUssdRequest ne cible pas
    // fiablement la bonne SIM sur un appareil donné, remplacer/étendre UssdExecutorImpl
    // par une implémentation basée sur un AccessibilityService qui lit le contenu de la
    // boîte de dialogue USSD système et y injecte les réponses successives, en
    // sélectionnant la SIM via l'UI système (ACTION_CALL + EXTRA_PHONE_ACCOUNT_HANDLE)
    // plutôt que via l'API sendUssdRequest. L'interface UssdExecutor/UssdExecutionCallback
    // reste inchangée pour l'appelant.
}

interface UssdExecutionCallback {
    fun onUssdResult(message: String)
    fun onUssdFailed(failureCode: Int)
}
