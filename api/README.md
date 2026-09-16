# API — Orchestrateur d'automatisation de paiements mobile money

API Spring Boot qui reçoit des demandes d'opérations mobile money (**DEPOSIT** /
**WITHDRAW**), sélectionne un appareil Android multi-SIM disponible avec la bonne puce
pour l'opérateur demandé, envoie la commande via Firebase Cloud Messaging, et suit la
progression de l'exécution à travers plusieurs mises à jour de statut successives
(reçue, en cours, terminée) plutôt qu'un seul callback final.

## Stack

- Java 17, Spring Boot 3.3, Maven
- Spring Web, Spring Data JPA, Spring Security (API key)
- PostgreSQL (H2 pour les tests)
- Firebase Admin SDK (FCM)

## Démarrage local

### 1. Base de données

```bash
docker compose up -d
```

Démarre Postgres sur `localhost:5432` (db `ussd_automation`, user/password `ussd_automation`).
Les tables sont créées automatiquement au démarrage (`ddl-auto: update`). Deux scripts
manuels complètent le schéma :

```bash
docker compose exec -T postgres psql -U ussd_automation -d ussd_automation < db/schema-extra.sql
docker compose exec -T postgres psql -U ussd_automation -d ussd_automation < db/seed-ussd-templates.sql
```

- `schema-extra.sql` crée l'index unique partiel garantissant un seul template USSD
  **actif** par couple `(operator, operationType)` — non exprimable via les annotations
  JPA utilisées avec `ddl-auto: update`.
- `seed-ussd-templates.sql` contient des exemples **fictifs** à remplacer par les codes
  marchand réels (ou à créer via `POST /api/ussd-templates`).

### 2. Configuration Firebase

1. [Console Firebase](https://console.firebase.google.com/) → Paramètres du projet →
   Comptes de service → Générer une nouvelle clé privée.
2. Placez le fichier JSON à `src/main/resources/firebase-service-account.json` (ignoré
   par `.gitignore`), ou pointez dessus via `FIREBASE_CREDENTIALS_PATH`.
3. L'app Android doit utiliser le **même projet Firebase**.

### 3. Clé API marchand

```bash
export MERCHANT_API_KEY=une-cle-secrete-longue
```

### 4. Lancer l'API / les tests

```bash
mvn spring-boot:run
mvn test
```

`mvn test` inclut `TransactionLifecycleIntegrationTest` : un test bout-en-bout (MockMvc +
H2 + FCM mocké) qui rejoue tout le cycle de vie d'une transaction — création, callbacks
successifs (`RECEIVED_BY_DEVICE`→`EXECUTING`→`SUCCESS`), idempotence sur un doublon de
statut, historique, libération du device, réconciliation par filtre, rejet des statuts
réservés à l'API et des callbacks d'un device non assigné, et le job de timeout. C'est le
niveau de test le plus complet possible dans cet environnement : l'exécution USSD/SMS
réelle côté Android nécessite un appareil ou émulateur physique, non disponible ici.

## Authentification

- `X-Api-Key: <MERCHANT_API_KEY>` — toutes les routes sauf le callback de statut.
- `X-Device-Api-Key: <clé du device>` — `POST /api/transactions/{id}/status` (vérifié
  comme correspondant au device réellement assigné à la transaction), et acceptée en
  plus de `X-Api-Key` sur `GET /api/devices/{id}/sim-slots` (l'app l'appelle elle-même
  au démarrage).

## Diagramme des transitions de statut

```
PENDING ──(template résolu, device+slot assignés)──▶ SENT_TO_DEVICE
                                                            │
                                     (FCM reçu, avant exécution USSD)
                                                            ▼
                                                   RECEIVED_BY_DEVICE
                                                            │
                                          (UssdExecutor déclenché côté app)
                                                            ▼
                                                       EXECUTING
                                                    ╱               ╲
                                    (SMS confirmé)╱                   ╲ (échec USSD/SMS)
                                                ▼                       ▼
                                            SUCCESS                  FAILED

Job de timeout (@Scheduled, 30s) :
  SENT_TO_DEVICE ──(> 90s sans RECEIVED_BY_DEVICE)──▶ TIMEOUT
  EXECUTING      ──(> 90s sans SUCCESS/FAILED)──────▶ TIMEOUT
```

Chaque transition (sauf `PENDING`/`SENT_TO_DEVICE`/`TIMEOUT`, pilotées par l'API) passe
par `POST /transactions/{id}/status` et est journalisée dans
`transaction_status_history`, sans jamais écraser l'historique. Un même statut reçu
deux fois de suite pour une transaction est ignoré (idempotence), avec un simple log.

## Endpoints

### Transactions

```http
POST /api/transactions
X-Api-Key: une-cle-secrete-longue
Content-Type: application/json

{
  "operationType": "DEPOSIT",
  "phone": "677000000",
  "operator": "MTN",
  "amount": 5000,
  "senderName": "Jean Dupont",
  "countryCode": "CM",
  "description": "Paiement commande #123"
}
```

```http
POST /api/transactions
X-Api-Key: une-cle-secrete-longue
Content-Type: application/json

{
  "operationType": "WITHDRAW",
  "phone": "690000000",
  "operator": "ORANGE",
  "amount": 2000,
  "senderName": "Jean Dupont",
  "countryCode": "CM"
}
```

Réponse `201 Created` (`TransactionResponse`, avec `statusHistory`) :

```json
{
  "id": "b3b1e2b2-9c2b-4e7a-9a2b-3f7a2c9b1234",
  "operationType": "DEPOSIT",
  "phone": "677000000",
  "operator": "MTN",
  "countryCode": "CM",
  "amount": 5000,
  "senderName": "Jean Dupont",
  "description": "Paiement commande #123",
  "statut": "SENT_TO_DEVICE",
  "deviceId": "0c1a2f3e-...",
  "simSlotUsed": 0,
  "ussdCodeUsed": "*126*1*5000*677000000#",
  "createdAt": "2026-09-16T10:00:00Z",
  "updatedAt": "2026-09-16T10:00:00Z",
  "expiresAt": "2026-09-16T10:01:30Z",
  "statusHistory": [
    { "statut": "PENDING", "receivedAt": "2026-09-16T10:00:00Z" },
    { "statut": "SENT_TO_DEVICE", "receivedAt": "2026-09-16T10:00:00Z" }
  ]
}
```

```http
GET /api/transactions/{id}                     (X-Api-Key)
GET /api/transactions?operationType=WITHDRAW&operator=MTN&statut=SUCCESS&from=...&to=...
```

La liste retourne `TransactionSummaryResponse` (sans `statusHistory`, pour rester légère
sur de gros volumes — utile pour la réconciliation comptable DEPOSIT vs WITHDRAW).

### Callback de statut (appelé par l'app à chaque changement)

```http
POST /api/transactions/{id}/status
X-Device-Api-Key: <clé du device assigné>
Content-Type: application/json

{
  "status": "EXECUTING",
  "timestamp": "2026-09-16T10:00:05Z"
}
```

Puis, plus tard :

```json
{
  "status": "SUCCESS",
  "timestamp": "2026-09-16T10:00:35Z",
  "rawSmsContent": "Vous avez recu un depot de 5000 XAF...",
  "operatorReference": "MP240916.1000.A12345"
}
```

`status` doit être `RECEIVED_BY_DEVICE`, `EXECUTING`, `SUCCESS` ou `FAILED` (les autres
valeurs sont rejetées en `400`, réservées à l'API elle-même).

### Templates USSD

```http
POST /api/ussd-templates
X-Api-Key: une-cle-secrete-longue
{ "operator": "MTN", "operationType": "DEPOSIT", "template": "*126*1*{amount}*{phone}#" }

GET /api/ussd-templates?operator=MTN[&operationType=DEPOSIT]

PUT /api/ussd-templates/{id}
{ "template": "*126*1*{amount}*{phone}#", "actif": true }
```

Créer un nouveau template pour un couple `(operator, operationType)` désactive
automatiquement l'ancien template actif de ce couple (l'historique reste consultable).

### Puces SIM

```http
POST /api/devices/{deviceId}/sim-slots
X-Api-Key: une-cle-secrete-longue
[
  { "slotIndex": 0, "operator": "MTN", "phoneNumberOnSim": "677000000" },
  { "slotIndex": 1, "operator": "ORANGE", "phoneNumberOnSim": "690000000" }
]

GET /api/devices/{deviceId}/sim-slots           (X-Api-Key ou X-Device-Api-Key)

PUT /api/devices/{deviceId}/sim-slots/{slotIndex}
{ "actif": false }
```

Toute modification (remplacement complet ou patch d'un slot) déclenche l'envoi d'un
message FCM `SIM_CONFIG_UPDATED` **ciblé sur le token de ce device** (pas un topic FCM
partagé, la config étant propre à chaque appareil), pour resynchronisation immédiate.

## Job de timeout

Toutes les 30s, les transactions bloquées en `SENT_TO_DEVICE` ou `EXECUTING` depuis plus
de 90s basculent en `TIMEOUT` (avec entrée d'historique), et leur device est libéré.

## Modèle de données

Voir `src/main/java/com/ussdauto/api/domain/entity` :
`Transaction`, `TransactionStatusHistory` (append-only), `Device`, `DeviceSimSlot`,
`UssdTemplate`.

## CI/CD

Workflows GitHub Actions définis à la racine du repo (`.github/workflows/`, requis pour
être pris en compte — un workflow placé dans `api/.github/` n'est pas exécuté), filtrés
sur les changements dans `api/**` :

- **`api-ci.yml`** — sur chaque push/PR : `mvn test` (profil `test`, H2 en mémoire,
  aucune dépendance externe). Publie les rapports Surefire en artefact.
- **`api-cd.yml`** — sur push vers `main` ou tag `v*` : rejoue les tests, puis build et
  push l'image Docker (`api/Dockerfile`, build multi-stage Maven → JRE) vers Docker Hub
  (`docker.io/$DOCKERHUB_NAMESPACE/ussd-auto:latest` et `:<tag>`).

Secrets requis côté GitHub (Settings → Secrets and variables → Actions) :
`DOCKERHUB_USERNAME`, `DOCKERHUB_TOKEN`, `DOCKERHUB_NAMESPACE`.

Build/lancement de l'image en local :

```bash
docker build -t ussd-auto:local ./api
docker run -p 8080:8080 --env-file api/.env ussd-auto:local
```
