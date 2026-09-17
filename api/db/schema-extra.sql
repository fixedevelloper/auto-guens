-- Contraintes non exprimables via les annotations JPA (Hibernate ddl-auto=update ne les
-- génère pas). À exécuter une fois après le premier démarrage de l'API (qui crée les tables).
-- Idempotent : peut être rejoué sans risque sur n'importe quel environnement.

-- ussd_template.country_code a été ajoutée après le lancement initial du projet. Sur une
-- base déjà peuplée, Hibernate ne peut pas ajouter directement une colonne NOT NULL (les
-- lignes existantes n'ont pas de valeur) — il échoue silencieusement et l'app démarre
-- quand même SANS la colonne. On la crée nullable, on backfill, puis on contraint.
ALTER TABLE ussd_template ADD COLUMN IF NOT EXISTS country_code varchar(2);
UPDATE ussd_template SET country_code = 'CM' WHERE country_code IS NULL;
ALTER TABLE ussd_template ALTER COLUMN country_code SET NOT NULL;

-- Un seul template ACTIF par (operator, operationType, countryCode) — un même operator
-- (ex. MTN) a un code USSD marchand différent selon le pays ; les versions désactivées
-- restent en base pour audit/historique.
DROP INDEX IF EXISTS uq_ussd_template_active;
CREATE UNIQUE INDEX IF NOT EXISTS uq_ussd_template_active
    ON ussd_template (operator, operation_type, country_code)
    WHERE actif = true;
