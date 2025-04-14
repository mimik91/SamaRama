DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'fk54ltwa6e3wnikmskpdg6mokvk'
        AND conrelid = 'service_orders'::regclass
    ) THEN
        ALTER TABLE service_orders
        ADD CONSTRAINT fk_service_orders_incomplete_bike
        FOREIGN KEY (bicycle_id)
        REFERENCES incomplete_bikes(id);
    END IF;
END $$;