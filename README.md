# Automatisation USSD — Paiements mobile money (multi-SIM)

Système d'automatisation de paiements mobile money (**DEPOSIT** / **WITHDRAW**) composé
de trois projets :

- [`api/`](api/README.md) — orchestrateur Spring Boot : reçoit les demandes d'opération,
  résout le code USSD marchand à composer, sélectionne un appareil Android disponible
  avec la bonne puce SIM pour l'opérateur demandé, envoie la commande via Firebase Cloud
  Messaging, et suit la progression à travers plusieurs mises à jour de statut
  successives (historisées, pas un seul callback final).
- [`android/`](android/README.md) — exécuteur Kotlin multi-SIM installé sur des
  appareils Android dédiés : crée sa copie locale (Room) de la transaction, compose le
  code USSD reçu sur la bonne puce, écoute et analyse le SMS de confirmation, puis
  notifie l'API à chaque changement de statut.
- [`web/`](web/README.md) — administration Next.js (accès par mot de passe) pour
  provisionner les devices/puces SIM et consulter les transactions traitées par l'API.

## Flux global

```
Marchand → POST /api/transactions (DEPOSIT|WITHDRAW, operator, phone, amount, ...)
             │
             ├─ résout le template USSD (operator, operationType) → code USSD
             ├─ sélectionne un device ONLINE avec une puce SIM active pour cet operator
             └─ envoie {transactionId, operationType, phone, operator, amount,
                        senderName, countryCode, description, simSlotIndex, ussdCode} via FCM
                          │
                          ▼
        App Android : insère la transaction en base locale (Room, idempotent),
        notifie RECEIVED_BY_DEVICE, résout le subscriptionId Android pour le
        simSlotIndex reçu, compose le code USSD tel quel sur cette SIM, notifie
        EXECUTING, écoute le SMS opérateur, l'analyse par opérateur (MtnSmsParser /
        OrangeSmsParser), vérifie qu'il confirme bien le type d'opération attendu
                          │
                          ▼
        POST /api/transactions/{id}/status (à CHAQUE changement : RECEIVED_BY_DEVICE,
        EXECUTING, SUCCESS|FAILED) → API journalise dans l'historique, met à jour le
        statut courant, libère le device sur statut final, publie un event de résultat
```

Points clés de conception :
- **L'API décide** (via `UssdTemplate`, un couple `(operator, operationType)` → code
  USSD, et `DeviceSimSlot` pour le choix de la puce), **l'app exécute** sans jamais
  construire elle-même de code USSD ni choisir un opérateur.
- La progression d'une transaction est **historisée** (`TransactionStatusHistory` côté
  API, `TransactionLocalEntity` côté app), pas résumée à un seul aller-retour.
- La config des puces SIM de chaque device est **gérée depuis l'API**
  (`DeviceSimSlot`) et synchronisée vers l'app (au démarrage + push FCM
  `SIM_CONFIG_UPDATED`), jamais éditée localement.

Voir chaque README pour le setup détaillé (Firebase, base de données, diagramme des
transitions de statut, exemples de payloads, permissions Android).
