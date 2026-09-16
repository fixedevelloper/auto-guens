# Android — Exécuteur de commandes USSD (multi-SIM)

Application Kotlin qui tourne sur des appareils Android dédiés et multi-SIM (parc de
téléphones marchand), reçoit des commandes d'exécution USSD via Firebase Cloud
Messaging, crée sa propre copie locale de la transaction (Room), compose le code USSD
**déjà résolu par l'API** sur la bonne puce SIM, écoute le SMS de confirmation de
l'opérateur, l'analyse, puis notifie l'API à **chaque** changement de statut (pas
seulement à la fin).

L'app ne décide jamais elle-même quel code USSD composer, ni quel opérateur associer à
quelle transaction : ces choix sont faits côté API (`UssdTemplateResolver`,
`DeviceSelectionService`) et transmis tels quels dans la commande FCM, y compris le
`simSlotIndex` logique à utiliser.

## Stack

- Kotlin, Jetpack Compose, ViewModel, Coroutines, Hilt
- Room (base locale)
- Firebase Cloud Messaging
- Retrofit + OkHttp
- WorkManager
- Timber (logging structuré)

## Architecture (MVVM)

```
ui/          — Activities, Composables, ViewModels (StatusActivity, SimSlotsActivity)
domain/      — modèles métier + interfaces (SmsParser, UssdExecutor, SimSlotManager,
               StatusNotifier, StatusCallbackRepository, TransactionLocalRepository,
               PendingTransactionRegistry)
data/        — implémentations concrètes :
               - local/db/  : entités et DAO Room (TransactionLocalEntity, SimSlotConfigLocalEntity)
               - remote/    : Retrofit (ApiService) + DTOs
               - repository/: implémentations Room/réseau des interfaces domain
               - sim/       : SimSlotManagerImpl + ActiveSimProvider (SubscriptionManager)
               - sms/       : SmsReceiver + parseurs par opérateur + config regex
               - status/    : StatusNotifierImpl
               - ussd/      : UssdExecutorImpl (TelephonyManager multi-SIM)
service/     — FirebaseMessagingService + Foreground Service d'exécution
worker/      — StatusNotifierWorker (retry WorkManager) + StatusNotifierRetryPolicy
```

## Flux d'exécution d'une commande

1. `UssdFirebaseMessagingService.onMessageReceived` reçoit un message data-only
   distingué par le champ `messageType` :
   - `TRANSACTION_COMMAND` : `{transactionId, operationType, phone, operator, amount,
     senderName, countryCode, description, simSlotIndex, ussdCode}`.
   - `SIM_CONFIG_UPDATED` : déclenche `SimSlotManager.syncFromRemote()`.
2. Pour une commande de transaction : insertion **idempotente** dans Room
   (`TransactionLocalRepository.insertIfNew` — une livraison FCM dupliquée du même
   `transactionId` est ignorée sans relancer l'exécution), puis notification à l'API du
   statut `RECEIVED_BY_DEVICE` **avant même** de composer l'USSD, puis démarrage du
   foreground service d'exécution.
3. `UssdExecutionForegroundService` résout le `subscriptionId` Android correspondant au
   `simSlotIndex` logique via `SimSlotManager`. Si aucune SIM physique ne correspond
   actuellement à ce slot → statut `FAILED` / `SIM_SLOT_UNAVAILABLE`, sans tenter
   l'exécution.
4. Notifie `EXECUTING`, puis appelle `UssdExecutor.execute(ussdCode, subscriptionId,
   ...)` (`TelephonyManager.createForSubscriptionId(subscriptionId).sendUssdRequest`).
5. En cas d'échec immédiat de composition → `FAILED` / `USSD_EXECUTION_FAILED`.
   Sinon, attend jusqu'à 60s qu'un SMS pertinent soit analysé par `SmsReceiver` (filtré
   par expéditeur connu et, quand disponible, par la SIM/`subscription` réceptrice) —
   sans SMS dans ce délai → `FAILED` / `NO_SMS_RECEIVED`.
6. `SmsParser` (par opérateur) vérifie que le SMS confirme bien l'opération attendue
   (garde-fou anti-erreur DEPOSIT/WITHDRAW) et extrait montant/référence.
7. **Chaque** changement de statut (`RECEIVED_BY_DEVICE`, `EXECUTING`, `SUCCESS`,
   `FAILED`) passe par `StatusNotifier.notifyStatusChange`, qui met à jour Room puis
   programme l'envoi vers l'API via `StatusNotifierWorker` (WorkManager, 3 tentatives,
   backoff exponentiel). `lastStatusSentAt` est mis à jour une fois l'envoi confirmé.
8. L'écran de statut affiche la dernière transaction locale (badge DÉPÔT/RETRAIT +
   statut) ; un écran séparé affiche la config des puces SIM en lecture seule et les
   écarts détectés avec les SIM physiquement présentes.

## Base locale (Room)

`TransactionLocalEntity` est la source de vérité pendant l'exécution, indépendamment de
la connectivité réseau — elle permet de retenter l'envoi du statut si la connexion
tombe, sans perdre le fil de l'exécution en cours. `SimSlotConfigLocalEntity` reflète la
configuration SIM synchronisée depuis l'API (`GET /devices/{deviceId}/sim-slots`), au
démarrage de l'app et à chaque réception de `SIM_CONFIG_UPDATED`.

## Regex des SMS externalisées

`res/raw/sms_parser_patterns.json` contient, par opérateur : expéditeurs reconnus,
mots-clés distinguant DEPOSIT/WITHDRAW, et regex d'extraction (montant, référence).
`MtnSmsParser` et `OrangeSmsParser` partagent la même logique de rapprochement
(`RegexBasedSmsParser`) — seules les regex diffèrent.

## Point d'extension : ciblage SIM et menus USSD multi-étapes

`UssdExecutorImpl` cible une SIM précise via
`TelephonyManager.createForSubscriptionId(subscriptionId).sendUssdRequest()`, mais la
fiabilité de ce ciblage dépend de la version d'Android et de l'OEM, et cette API ne gère
qu'un aller-retour USSD simple. Si un opérateur exige un menu à plusieurs étapes (choix
DEPOSIT/WITHDRAW dans un sous-menu, saisie d'un code PIN marchand), ou si le ciblage par
`subscriptionId` n'est pas fiable sur un appareil donné, remplacer/étendre
`UssdExecutorImpl` par une implémentation `AccessibilityService` qui pilote la boîte de
dialogue USSD système (et sélectionne la SIM via l'UI d'appel,
`ACTION_CALL`+`EXTRA_PHONE_ACCOUNT_HANDLE`, plutôt que via `sendUssdRequest`). L'interface
`UssdExecutor`/`UssdExecutionCallback` reste inchangée pour l'appelant — voir le
commentaire sur `domain/ussd/UssdExecutor.kt`.

## Configuration Firebase

1. [Console Firebase](https://console.firebase.google.com/) → ajouter une app Android
   avec le package `com.ussdauto.android`, dans **le même projet** que l'API.
2. Téléchargez `google-services.json` et placez-le à `app/google-services.json`
   (remplace le template `app/google-services.json.example` fourni, ignoré par git).

## Provisioning d'un device

Aucun endpoint d'auto-enregistrement de device n'existe dans le périmètre actuel de
l'API. Pour mettre en service un appareil :

1. Créer manuellement la ligne `Device` côté API (apiKey, token FCM) et sa config
   `DeviceSimSlot` (`POST /api/devices/{deviceId}/sim-slots`).
2. Sur l'appareil, renseigner `deviceId` et `apiKey` dans `DeviceCredentialsStore`
   (`data/local/DeviceCredentialsStore.kt`) — via un écran d'admin à ajouter, ou en dur
   au premier lancement pour un parc de test.
3. Le token FCM (`onNewToken`) doit être communiqué à l'API pour tenir `Device.fcmToken`
   à jour — extension future documentée dans le code, hors périmètre des endpoints
   actuels.
4. Au premier lancement, accepter la demande de permission `READ_PHONE_STATE` (déclenchée
   par `StatusActivity`) : sans elle, `SubscriptionManager` ne peut lister aucune SIM et
   la détection automatique des SIM échoue silencieusement.

## Permissions (justification Play Store)

| Permission | Pourquoi |
|---|---|
| `RECEIVE_SMS` | Recevoir en temps réel le SMS de confirmation de l'opérateur mobile money pour valider l'opération DEPOSIT/WITHDRAW. |
| `READ_SMS` | Relire le contenu des SMS déjà présents si nécessaire, en complément de la réception temps réel. |
| `CALL_PHONE` | Requis par `TelephonyManager.sendUssdRequest()` pour composer le code USSD marchand. |
| `READ_PHONE_STATE` | Lister les SIM actives par slot (`SubscriptionManager`) pour cibler la bonne puce lors de l'exécution multi-SIM. |
| `INTERNET` / `ACCESS_NETWORK_STATE` | Recevoir les commandes FCM, synchroniser la config SIM, et renvoyer les statuts à l'API. |
| `FOREGROUND_SERVICE` / `FOREGROUND_SERVICE_PHONE_CALL` | Exécuter la composition USSD et l'attente du SMS de façon fiable, y compris écran éteint. |
| `RECEIVE_BOOT_COMPLETED` | Redémarrer l'écoute FCM après un reboot de l'appareil dédié. |
| `WAKE_LOCK` | Empêcher la mise en veille pendant l'exécution d'une commande USSD. |

Ces appareils sont des téléphones **dédiés et détenus par le marchand**, jamais des
téléphones d'utilisateurs finaux — c'est cette justification qui doit être fournie lors
de la revue Play Store pour les permissions SMS/Call/Phone State sensibles.

## Tests

```bash
./gradlew testDebugUnitTest
```

- `MtnSmsParserTest` / `OrangeSmsParserTest` : succès DEPOSIT, succès WITHDRAW, échec
  opérateur, format de SMS inattendu, mismatch de type d'opération.
- `SimSlotManagerImplTest` : résolution du `subscriptionId` depuis un `slotIndex`
  logique, détection d'écarts SIM physique/config (via `ActiveSimProvider`, une petite
  abstraction au-dessus de `SubscriptionManager` pour rester testable en JVM pur sans
  Robolectric).
- `TransactionLocalRepositoryImplTest` : idempotence de l'insertion locale (une
  livraison FCM dupliquée du même `transactionId` ne doit pas relancer l'exécution).
- `StatusNotifierRetryPolicyTest` : bornes de la logique de retry (3 tentatives au
  total). Le `Worker` WorkManager lui-même n'est pas testé en JVM pur (nécessiterait
  Robolectric ou un test instrumenté) — la décision de retry est donc extraite dans une
  classe pure dédiée pour rester testable.
