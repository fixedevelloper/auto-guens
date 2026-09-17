-- Seed manuel des templates USSD (exemples fictifs — à remplacer par les codes marchand réels
-- fournis par chaque opérateur, par pays). À exécuter après db/schema-extra.sql, par exemple :
-- docker compose exec -T postgres psql -U ussd_automation -d ussd_automation < db/seed-ussd-templates.sql

INSERT INTO ussd_template (id, operator, operation_type, country_code, template, actif, created_at, updated_at)
VALUES (gen_random_uuid(), 'MTN', 'DEPOSIT', 'CM', '*126*1*{amount}*{phone}#', true, now(), now())
ON CONFLICT DO NOTHING;

INSERT INTO ussd_template (id, operator, operation_type, country_code, template, actif, created_at, updated_at)
VALUES (gen_random_uuid(), 'MTN', 'WITHDRAW', 'CM', '*126*2*{amount}*{phone}#', true, now(), now())
ON CONFLICT DO NOTHING;

INSERT INTO ussd_template (id, operator, operation_type, country_code, template, actif, created_at, updated_at)
VALUES (gen_random_uuid(), 'ORANGE', 'DEPOSIT', 'CM', '#150*50*{amount}*{phone}#', true, now(), now())
ON CONFLICT DO NOTHING;

INSERT INTO ussd_template (id, operator, operation_type, country_code, template, actif, created_at, updated_at)
VALUES (gen_random_uuid(), 'ORANGE', 'WITHDRAW', 'CM', '#150*51*{amount}*{phone}#', true, now(), now())
ON CONFLICT DO NOTHING;
