-- V2.0__insert_initial_data.sql
-- Inicjalne dane dla aplikacji

-- ==========================================
-- ENUMERACJE ROWEROWE
-- ==========================================

-- Wstawienie podstawowych enumeracji
INSERT INTO bicycle_enumerations (type) VALUES
    ('BRAND'),
    ('BIKE_TYPE'),
    ('FRAME_MATERIAL'),
    ('CITY'),
    ('ORDER_STATUS'),
    ('DEFAULT_TRANSPORT_PRICE');

-- Marki rowerów
INSERT INTO bicycle_enumeration_values (enumeration_id, value)
SELECT id, unnest(ARRAY[
    'Trek', 'Specialized', 'Giant', 'Cannondale', 'Scott',
    'Merida', 'Cube', 'Canyon', 'Bianchi', 'BMC',
    'Kross', 'Orbea', 'Ghost', 'Fuji', 'GT',
    'Pinarello', 'Cervelo', 'Focus', 'Felt', 'Lapierre',
    'Romet', 'Kellys', 'Unibike', 'Kona', 'Marin',
    'Santa Cruz', 'Norco', 'Commencal', 'YT', 'Devinci', 'Inny', 'Hercules'
])
FROM bicycle_enumerations WHERE type = 'BRAND';

-- Typy rowerów
INSERT INTO bicycle_enumeration_values (enumeration_id, value)
SELECT id, unnest(ARRAY[
    'Górski (MTB)', 'Szosowy', 'Gravel', 'Miejski',
    'Trekkingowy', 'BMX', 'Dziecięcy', 'Elektryczny',
    'Fatbike', 'Crossowy', 'Fitness', 'Składany'
])
FROM bicycle_enumerations WHERE type = 'BIKE_TYPE';

-- Materiały ramowe
INSERT INTO bicycle_enumeration_values (enumeration_id, value)
SELECT id, unnest(ARRAY[
    'Aluminium', 'Karbon (carbon)', 'Stal', 'Tytan',
    'Chrom-molibden', 'Magnez', 'Kompozyt'
])
FROM bicycle_enumerations WHERE type = 'FRAME_MATERIAL';

-- Miasta
INSERT INTO bicycle_enumeration_values (enumeration_id, value)
SELECT id, unnest(ARRAY[
    'Kraków', 'Węgrzce', 'Zielonki', 'Bosutów', 'Bibice',
    'Batowice', 'Michałowice', 'Dziekanowice', 'Raciborowice', 'Boleń'
])
FROM bicycle_enumerations WHERE type = 'CITY';

-- Statusy zamówień
INSERT INTO bicycle_enumeration_values (enumeration_id, value)
SELECT id, unnest(ARRAY[
    'PENDING', 'CONFIRMED', 'PICKED_UP', 'IN_SERVICE', 'ON_THE_WAY_BACK', 'FINISHED', 'CANCELLED'
])
FROM bicycle_enumerations WHERE type = 'ORDER_STATUS';

-- Domyślna cena transportu
INSERT INTO bicycle_enumeration_values (enumeration_id, value)
SELECT id, unnest(ARRAY['30'])
FROM bicycle_enumerations WHERE type = 'DEFAULT_TRANSPORT_PRICE';

-- ==========================================
-- DOMYŚLNY SERWIS
-- ==========================================

-- Wstawienie domyślnego serwisu
INSERT INTO bike_services (
    id,
    name,
    street,
    building,
    city,
    transport_cost,
    transport_available,
    created_at,
    updated_at
) VALUES (
    2137,
    'Serwis Cyclopick.pl',
    'Kiełkowskiego',
    '10b',
    'Kraków',
    0.00,
    true,
    NOW(),
    NOW()
);


-- ==========================================
-- KATEGORIE POKRYĆ NAPRAW
-- ==========================================

-- Populowanie kategorii pokryć napraw
INSERT INTO bike_repair_coverage_category (name, display_order) VALUES
    ('Typ roweru', 1),
    ('Rodzaj usługi', 2),
    ('Systemy e-bike', 3),
    ('Komponenty',  4),
    ('Amortyzacja',  5),
    ('Udogodnienia',  6),
    ('Oficjalny Partner', 7);

-- ==========================================
-- PRZYKŁADOWE KUPONY
-- ==========================================

-- Przykładowy kupon testowy
INSERT INTO coupons (coupon_code, price_after_discount, expiration_date, usage_count) VALUES
    ('TEST1906', 25.00, '2025-09-30', 0);

-- ==========================================
-- SEKWENCJE
-- ==========================================

-- Ustaw sekwencję na wartość większą niż najwyższe ID
SELECT setval('bike_services_id_seq', GREATEST(2138, (SELECT COALESCE(MAX(id), 0) + 1 FROM bike_services)));