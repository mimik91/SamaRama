
BEGIN
    -- Sprawdzamy czy kolumna już istnieje
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'bicycles'
        AND column_name = 'frame_material'
    ) THEN
        -- Dodajemy kolumnę tylko jeśli nie istnieje
        ALTER TABLE bicycles ADD COLUMN frame_material VARCHAR(50);
    END IF;
END $$;