# Déploiement serveur

`docker-compose.yml` de production : API (image publiée par `api-cd.yml`) + Web admin
(image publiée par `web-cd.yml`) + PostgreSQL.

Le reverse proxy public (TLS/Certbot compris) est le **Nginx système** du serveur, pas un
service dockerisé — aucun conteneur n'est publié sur `0.0.0.0`, seulement sur
`127.0.0.1`. `API_PORT` doit être identique à `server.port` dans
`api/src/main/resources/application.yml` (actuellement **8089** sur ce serveur — le 8080
par défaut de Spring Boot y est déjà occupé par un autre service).

Exemple de blocs `location`/`server` côté Nginx système, un sous-domaine par service :

```nginx
# API — ussd.guens.org
location / {
    proxy_pass http://127.0.0.1:8089;
    proxy_set_header Host              $host;
    proxy_set_header X-Real-IP         $remote_addr;
    proxy_set_header X-Forwarded-For   $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
}

location /actuator/health {
    proxy_pass http://127.0.0.1:8089/actuator/health;
    access_log off;
}
```

```nginx
# Web admin — admin.guens.org (ou un autre sous-domaine dédié)
location / {
    proxy_pass http://127.0.0.1:3030;
    proxy_set_header Host              $host;
    proxy_set_header X-Real-IP         $remote_addr;
    proxy_set_header X-Forwarded-For   $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
}
```

## 1. Configuration

```bash
cp .env.example .env
# éditer .env : POSTGRES_PASSWORD, MERCHANT_API_KEY, DOCKERHUB_NAMESPACE,
#               FIREBASE_CREDENTIALS_FILE, ADMIN_PASSWORD, SESSION_SECRET
```

`FIREBASE_CREDENTIALS_FILE` doit pointer vers le JSON de compte de service Firebase
présent sur le serveur (hors repo, jamais committé).

`ADMIN_PASSWORD` (mot de passe unique de connexion à l'admin web) et `SESSION_SECRET`
(signature du cookie de session, ex. `openssl rand -base64 32`) doivent être des valeurs
fortes et différentes de tout autre secret du projet.

## 2. Démarrage

```bash
docker compose pull
docker compose up -d
```

## 3. Schéma additionnel (première installation)

Le profil `prod` utilise `ddl-auto: update` : les tables sont créées automatiquement au
premier démarrage (comme en `dev`). Une fois l'API démarrée avec succès, compléter avec
les scripts de `../api/db/` :

```bash
docker compose exec -T postgres psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" < ../api/db/schema-extra.sql
docker compose exec -T postgres psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" < ../api/db/seed-ussd-templates.sql
```

`schema-extra.sql` crée un index unique partiel non exprimable via `ddl-auto: update`
(idempotent, `IF NOT EXISTS`).

## 4. Mise à jour

```bash
docker compose pull api web
docker compose up -d api web
```

## 5. Logs / état

```bash
docker compose ps
docker compose logs -f api
docker compose logs -f web
```
