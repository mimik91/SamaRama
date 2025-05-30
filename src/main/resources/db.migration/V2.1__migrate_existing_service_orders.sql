-- Migracja istniejących danych z service_orders do nowej struktury

-- 1. Przeniesienie danych do tabeli orders (kopiowanie wspólnych pól)
INSERT INTO orders (
    id, bicycle_id, client_id, pickup_date, pickup_address,
    pickup_latitude, pickup_longitude, status, order_date,
    price, additional_notes, last_modified_by, last_modified_date
)
SELECT
    id, bicycle_id, user_id, pickup_date, pickup_address,
    pickup_latitude, pickup_longitude, status, orderdate,
    price, additional_notes, last_modified_by, last_modified_date
FROM service_orders;

-- 2. Teraz service_orders stanie się tabelą dziecka
-- Najpierw tworzymy backup obecnej tabeli
CREATE TABLE service_orders_backup AS SELECT * FROM service_orders;

-- 3. Usuwamy wszystkie kolumny które są teraz w orders
ALTER TABLE service_orders
    DROP COLUMN bicycle_id,
    DROP COLUMN user_id,
    DROP COLUMN pickup_date,
    DROP COLUMN pickup_address,
    DROP COLUMN pickup_latitude,
    DROP COLUMN pickup_longitude,
    DROP COLUMN status,
    DROP COLUMN orderdate,
    DROP COLUMN price,
    DROP COLUMN additional_notes,
    DROP COLUMN last_modified_by,
    DROP COLUMN last_modified_date;

-- 4. Dodanie foreign key do orders
ALTER TABLE service_orders
    ADD CONSTRAINT fk_service_orders_order
    FOREIGN KEY (id) REFERENCES orders(id) ON DELETE CASCADE;

-- 5. Aktualizacja sekwencji ID dla tabeli orders
SELECT setval('orders_id_seq', (SELECT MAX(id) FROM orders));

-- Sprawdzenie poprawności migracji
SELECT
    'orders' as table_name, COUNT(*) as count FROM orders
UNION ALL
SELECT
    'service_orders' as table_name, COUNT(*) as count FROM service_orders
UNION ALL
SELECT
    'service_orders_backup' as table_name, COUNT(*) as count FROM service_orders_backup;

-- Po sprawdzeniu poprawności migracji możesz usunąć backup:
-- DROP TABLE service_orders_backup;