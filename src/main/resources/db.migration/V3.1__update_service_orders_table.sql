-- Add service_package_id column to service_orders table
ALTER TABLE service_orders ADD COLUMN service_package_id BIGINT;

-- Add service_package_code column to maintain backward compatibility
ALTER TABLE service_orders ADD COLUMN service_package_code VARCHAR(20);

-- Transfer existing enum values to the new code column
UPDATE service_orders SET service_package_code = service_package::varchar WHERE service_package IS NOT NULL;

-- Insert default service packages if they don't exist yet (this is a safety measure)
INSERT INTO service_packages (code, name, description, price, active, display_order)
SELECT 'BASIC', 'Przegląd podstawowy', 'Podstawowe sprawdzenie stanu roweru i regulacje', 200.00, TRUE, 1
WHERE NOT EXISTS (SELECT 1 FROM service_packages WHERE code = 'BASIC');

INSERT INTO service_packages (code, name, description, price, active, display_order)
SELECT 'EXTENDED', 'Przegląd rozszerzony', 'Rozszerzony przegląd z czyszczeniem i wymianą podstawowych części', 350.00, TRUE, 2
WHERE NOT EXISTS (SELECT 1 FROM service_packages WHERE code = 'EXTENDED');

INSERT INTO service_packages (code, name, description, price, active, display_order)
SELECT 'FULL', 'Przegląd pełny', 'Kompleksowy przegląd i konserwacja całego roweru', 600.00, TRUE, 3
WHERE NOT EXISTS (SELECT 1 FROM service_packages WHERE code = 'FULL');

-- Link existing orders to service packages based on code
UPDATE service_orders
SET service_package_id = sp.id
FROM service_packages sp
WHERE service_orders.service_package_code = sp.code;

-- Add foreign key constraint
ALTER TABLE service_orders
ADD CONSTRAINT fk_service_orders_service_package
FOREIGN KEY (service_package_id) REFERENCES service_packages(id);

-- Now that we've transferred the data, we can drop the old enum column
-- Note: In a production environment, it might be safer to keep the column for a while
ALTER TABLE service_orders DROP COLUMN IF EXISTS service_package;