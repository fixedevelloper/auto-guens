# Web — Administration USSD Automation

Application Next.js (App Router, TypeScript) pour :

- **Configurer les devices et leurs puces SIM** (créer un device, déclarer/activer-
  désactiver ses slots SIM).
- **Consulter les transactions** effectuées par l'API Spring (`../api`), avec filtres et
  détail (historique de statuts).

## Stack

- Next.js 16 (App Router), React 19, TypeScript, Tailwind CSS 4
- Authentification par mot de passe partagé unique (`ADMIN_PASSWORD`) — cookie de session
  signé (HMAC), sans base de données ni store côté serveur
- Aucun appel direct de l'API Spring depuis le navigateur : tout passe par des Server
  Components / Server Actions (`lib/api.ts`), qui portent `MERCHANT_API_KEY` — cette clé
  n'atteint jamais le client

## Démarrage local

```bash
cp .env.example .env.local
# éditer .env.local : API_BASE_URL (l'API Spring doit tourner), MERCHANT_API_KEY,
#                     ADMIN_PASSWORD, SESSION_SECRET

npm install
npm run dev
```

## Build / production

```bash
npm run build
npm run start   # ou, comme en prod (output: standalone) : node .next/standalone/server.js
```

Voir `../deploy/` pour le déploiement Docker (service `web` dans
`deploy/docker-compose.yml`) derrière le Nginx système du serveur.

## Structure

- `app/login/` — page de connexion (mot de passe unique)
- `app/(app)/` — pages protégées (garde d'auth dans `proxy.ts`, layout avec nav)
  - `transactions/` — liste (filtres via query params) + détail
  - `devices/` — liste + création + détail (config des puces SIM)
- `lib/api.ts` — client HTTP serveur-only vers l'API Spring
- `lib/session.ts` — cookie de session signé HMAC
- `lib/data/` — fonctions de lecture (Server Components)
- `lib/*-actions.ts` — Server Actions (mutations : login, création device, sim-slots)
- `proxy.ts` — garde d'authentification (équivalent `middleware.ts`, renommé en Next 16)

## Endpoints API consommés

Voir `../api/README.md` pour le détail. Résumé :

- `GET /api/transactions`, `GET /api/transactions/{id}`
- `GET /api/devices`, `GET /api/devices/{id}`, `POST /api/devices`
- `GET/POST /api/devices/{id}/sim-slots`, `PUT /api/devices/{id}/sim-slots/{slotIndex}`

Toujours avec le header `X-Api-Key: $MERCHANT_API_KEY` (jamais `X-Device-Api-Key`, réservé
à l'app Android).
