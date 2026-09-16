# Déploiement serveur

`docker-compose.yml` de production : Nginx (reverse proxy) + API (image publiée par
`api-cd.yml`) + PostgreSQL. Nginx est le seul service exposé publiquement ; l'API n'écoute
que sur le réseau interne Docker.

## 1. Configuration

```bash
cp .env.example .env
# éditer .env : POSTGRES_PASSWORD, MERCHANT_API_KEY, DOCKERHUB_NAMESPACE,
#               FIREBASE_CREDENTIALS_FILE, SERVER_NAME
```

`FIREBASE_CREDENTIALS_FILE` doit pointer vers le JSON de compte de service Firebase
présent sur le serveur (hors repo, jamais committé).

`SERVER_NAME` est le nom de domaine (ou IP) utilisé par Nginx dans `server_name` ;
laisser `_` pour accepter n'importe quel host (défaut).

## 2. Démarrage

```bash
docker compose pull
docker compose up -d
```

## 3. Schéma additionnel (première installation)

Le `ddl-auto: validate` du profil `prod` ne crée pas les tables : elles doivent déjà
exister (créées une fois via le profil `dev`/`update`, ou manuellement), puis complétées
par les scripts de `../api/db/` :

```bash
docker compose exec -T postgres psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" < ../api/db/schema-extra.sql
docker compose exec -T postgres psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" < ../api/db/seed-ussd-templates.sql
```

## 4. Mise à jour

```bash
docker compose pull api
docker compose up -d api
```

## 5. Logs / état

```bash
docker compose ps
docker compose logs -f api
docker compose logs -f nginx
```

## 6. HTTPS

Cette configuration Nginx sert en HTTP simple (port 80). Pour activer TLS (ex.
Let's Encrypt via `certbot`), ajouter un service `certbot` + un volume partagé pour les
certificats, monter `/etc/letsencrypt` dans le service `nginx`, et étendre
`nginx/templates/default.conf.template` avec un `server { listen 443 ssl; ... }` — non
inclus ici pour rester indépendant d'un nom de domaine.
