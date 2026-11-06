-- Barizone non ha più dati dimostrativi: le aziende sono solo quelle presenti nel database.
-- Eventuali schede ancora segnate come demo vengono eliminate prima di rimuovere la colonna.
DELETE FROM company_profiles WHERE is_demo;
ALTER TABLE company_profiles DROP COLUMN is_demo;
