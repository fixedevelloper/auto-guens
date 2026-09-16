-- Contraintes non exprimables via les annotations JPA (Hibernate ddl-auto=update ne les
-- génère pas). À exécuter une fois après le premier démarrage de l'API (qui crée les tables).

-- Un seul template ACTIF par (operator, operationType) ; les versions désactivées restent
-- en base pour audit/historique.
CREATE UNIQUE INDEX IF NOT EXISTS uq_ussd_template_active
    ON ussd_template (operator, operation_type)
    WHERE actif = true;
