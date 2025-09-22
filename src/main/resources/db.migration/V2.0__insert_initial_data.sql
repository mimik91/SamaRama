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
-- PAKIETY SERWISOWE
-- ==========================================

-- Wstawienie pakietów serwisowych
INSERT INTO service_packages (code, name, description, price, active, display_order)
VALUES
    ('BASIC', 'Przegląd podstawowy', 'Podstawowe sprawdzenie stanu roweru i regulacje', 200.00, true, 1),
    ('EXTENDED', 'Przegląd rozszerzony', 'Rozszerzony przegląd z czyszczeniem i wymianą podstawowych części', 350.00, true, 2),
    ('FULL', 'Przegląd pełny', 'Kompleksowy przegląd i konserwacja całego roweru', 600.00, true, 3);

-- Cechy pakietu BASIC
INSERT INTO service_package_features (package_id, feature)
SELECT id, unnest(ARRAY[
    'Ocena stanu technicznego roweru',
    'Regulacja hamulców',
    'Regulacja przerzutek',
    'Smarowanie łańcucha',
    'Sprawdzenie ciśnienia w ogumieniu',
    'Sprawdzenie poprawności skręcenia roweru',
    'Kontrola luzu sterów',
    'Kontrola połączeń śrubowych',
    'Sprawdzenie linek, pancerzy',
    'Sprawdzenie stanu opon',
    'Kasowanie luzów i regulacja elementów ruchomych'
])
FROM service_packages WHERE code = 'BASIC';

-- Cechy pakietu EXTENDED
INSERT INTO service_package_features (package_id, feature)
SELECT id, unnest(ARRAY[
    'Wszystkie elementy przeglądu podstawowego',
    'Czyszczenie i smarowanie łańcucha, kasety',
    'Wymiana smaru w sterach, piastach, suporcie',
    'Kontrola kół',
    'Kontrola działania amortyzatora',
    'W cenie wymiana klocków, linek, pancerzy, dętek, opon, łańcucha, kasety lub wolnobiegu. Do ceny należy doliczyć koszt części, które wymagają wymiany.'
])
FROM service_packages WHERE code = 'EXTENDED';

-- Cechy pakietu FULL
INSERT INTO service_package_features (package_id, feature)
SELECT id, unnest(ARRAY[
    'Wszystkie elementy przeglądu rozszerzonego',
    'Mycie roweru',
    'Czyszczenie i konserwacja przerzutek',
    'Czyszczenie i smarowanie łańcucha, kasety, korby',
    'Wymiana smaru w sterach, piastach, suporcie',
    'Wymiana linek i pancerzy',
    'Kontrola luzu łożysk suportu, steru, piast',
    'Sprawdzenie połączeń gwintowych',
    'Zewnętrzna konserwacja goleni amortyzatora',
    'Centrowanie kół',
    'Linki i pancerze oraz mycie roweru są wliczone w cenę przeglądu'
])
FROM service_packages WHERE code = 'FULL';

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