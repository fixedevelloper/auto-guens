# Déploiement serveur

`docker-compose.yml` de production : API (image publiée par `api-cd.yml`) + PostgreSQL.

Le reverse proxy public (TLS/Certbot compris) est le **Nginx système** du serveur, pas un
service dockerisé — l'API n'est publiée que sur `127.0.0.1:${API_PORT}` (par défaut
`8080`), jamais sur `0.0.0.0`. Exemple de bloc `location` côté Nginx système :

```nginx
location / {
    proxy_pass http://127.0.0.1:8080;
    proxy_set_header Host              $host;
    proxy_set_header X-Real-IP         $remote_addr;
    proxy_set_header X-Forwarded-For   $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
}

location /actuator/health {
    proxy_pass http://127.0.0.1:8080/actuator/health;
    access_log off;
}
```

## 1. Configuration

```bash
cp .env.example .env
# éditer .env : POSTGRES_PASSWORD, MERCHANT_API_KEY, DOCKERHUB_NAMESPACE,
#               FIREBASE_CREDENTIALS_FILE
```

`FIREBASE_CREDENTIALS_FILE` doit pointer vers le JSON de compte de service Firebase
présent sur le serveur (hors repo, jamais committé).

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
docker compose pull api
docker compose up -d api
```

## 5. Logs / état

```bash
docker compose ps
docker compose logs -f api
```
