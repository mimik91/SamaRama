
BEGIN;

-- 1. Handle service_records dependencies
-- First check if service_id column exists in service_records
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'service_records' AND column_name = 'service_id'
    ) THEN
        -- Remove the foreign key constraint if it exists
        ALTER TABLE service_records DROP CONSTRAINT IF EXISTS fk_service_records_service;

        -- Drop the service_id column
        ALTER TABLE service_records DROP COLUMN service_id;
    END IF;
END $$;

-- 2. Handle service_orders dependencies
-- Check if service_id column exists in service_orders
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'service_orders' AND column_name = 'service_id'
    ) THEN
        -- Remove the foreign key constraint if it exists
        ALTER TABLE service_orders DROP CONSTRAINT IF EXISTS fk_service_orders_service;

        -- Drop the service_id column
        ALTER TABLE service_orders DROP COLUMN service_id;
    END IF;
END $$;

-- 3. Handle map-related columns in service_orders
-- Check if pickup_latitude/longitude columns exist in service_orders
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'service_orders' AND column_name = 'pickup_latitude'
    ) THEN
        -- Remove the columns
        ALTER TABLE service_orders
        DROP COLUMN IF EXISTS pickup_latitude,
        DROP COLUMN IF EXISTS pickup_longitude;
    END IF;
END $$;

-- 4. Remove service_id from service_packages if it exists
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'service_packages' AND column_name = 'service_id'
    ) THEN
        -- First set service_id to NULL to avoid constraint issues
        UPDATE service_packages SET service_id = NULL WHERE service_id IS NOT NULL;

        -- Drop the service_id column
        ALTER TABLE service_packages DROP COLUMN service_id;
    END IF;
END $$;

-- 5. Finally, drop the bike_services and opening_hours tables
-- Drop opening_hours if it exists
DROP TABLE IF EXISTS opening_hours;

-- Drop bike_services if it exists
DROP TABLE IF EXISTS bike_services;

-- COMMIT TRANSACTION
COMMIT;