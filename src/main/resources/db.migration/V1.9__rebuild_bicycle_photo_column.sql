-- Całkowita przebudowa kolumny photo w tabeli bicycles

-- 1. Najpierw tworzymy nową kolumnę tymczasową
ALTER TABLE bicycles ADD COLUMN photo_new bytea;

-- 2. Kopiujemy dane ze starej do nowej kolumny (z rzutowaniem)
UPDATE bicycles SET photo_new = CASE
    WHEN photo IS NULL THEN NULL
    ELSE photo::bytea
    END;

-- 3. Usuwamy starą kolumnę
ALTER TABLE bicycles DROP COLUMN photo;

-- 4. Zmieniamy nazwę nowej kolumny na oryginalną
ALTER TABLE bicycles RENAME COLUMN photo_new TO photo;