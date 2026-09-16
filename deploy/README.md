# Déploiement serveur

`docker-compose.yml` de production : API (image publiée par `api-cd.yml`) + PostgreSQL.

## 1. Configuration

```bash
cp .env.example .env
# éditer .env : POSTGRES_PASSWORD, MERCHANT_API_KEY, DOCKERHUB_NAMESPACE, FIREBASE_CREDENTIALS_FILE
```

`FIREBASE_CREDENTIALS_FILE` doit pointer vers le JSON de compte de service Firebase
présent sur le serveur (hors repo, jamais committé).

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
```
