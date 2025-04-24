-- Create admin user
INSERT INTO incomplete_users (email, created_at) 
VALUES ('dominiklach@poczta.fm', CURRENT_TIMESTAMP);

INSERT INTO users (id, first_name, last_name, password, verified)
VALUES (
    (SELECT id FROM incomplete_users WHERE email = 'dominiklach@poczta.fm'),
    'Admin',
    'User',
    '$2a$10$JQOfPxoQlKw2YGjYkN6we.p2y2Nt3s5QFUKlL6Yi9P2livRuAyLqK', -- hashed 'misiek'
    true
);

INSERT INTO user_roles (user_id, role)
VALUES ((SELECT id FROM incomplete_users WHERE email = 'dominiklach@poczta.fm'), 'ROLE_ADMIN');

-- Initialize default bicycle enumerations
-- Brands
INSERT INTO bicycle_enumerations (type) VALUES ('BRAND');
INSERT INTO bicycle_enumeration_values (enumeration_id, value)
SELECT id, unnest(ARRAY[
    'Trek', 'Specialized', 'Giant', 'Cannondale', 'Scott',
    'Merida', 'Cube', 'Canyon', 'Bianchi', 'BMC',
    'Kross', 'Orbea', 'Ghost', 'Fuji', 'GT',
    'Pinarello', 'Cervelo', 'Focus', 'Felt', 'Lapierre',
    'Romet', 'Kellys', 'Unibike', 'Kona', 'Marin',
    'Santa Cruz', 'Norco', 'Commencal', 'YT', 'Devinci'
])
FROM bicycle_enumerations WHERE type = 'BRAND';

-- Bike types
INSERT INTO bicycle_enumerations (type) VALUES ('BIKE_TYPE');
INSERT INTO bicycle_enumeration_values (enumeration_id, value)
SELECT id, unnest(ARRAY[
    'Górski (MTB)', 'Szosowy', 'Gravel', 'Miejski',
    'Trekkingowy', 'BMX', 'Dziecięcy', 'Elektryczny',
    'Fatbike', 'Crossowy', 'Fitness', 'Składany'
])
FROM bicycle_enumerations WHERE type = 'BIKE_TYPE';

-- Frame materials
INSERT INTO bicycle_enumerations (type) VALUES ('FRAME_MATERIAL');
INSERT INTO bicycle_enumeration_values (enumeration_id, value)
SELECT id, unnest(ARRAY[
    'Aluminium', 'Karbon (carbon)', 'Stal', 'Tytan',
    'Chrom-molibden', 'Magnez', 'Kompozyt'
])
FROM bicycle_enumerations WHERE type = 'FRAME_MATERIAL';

-- Order statuses
INSERT INTO bicycle_enumerations (type) VALUES ('ORDER_STATUS');
INSERT INTO bicycle_enumeration_values (enumeration_id, value)
SELECT id, unnest(ARRAY[
    'PENDING', 'CONFIRMED', 'PICKED_UP', 'IN_SERVICE', 'COMPLETED', 'DELIVERED', 'CANCELLED'
])
FROM bicycle_enumerations WHERE type = 'ORDER_STATUS';

-- Initialize default service packages
INSERT INTO service_packages (code, name, description, price, active, display_order)
VALUES 
    ('BASIC', 'Przegląd podstawowy', 'Podstawowe sprawdzenie stanu roweru i regulacje', 200.00, true, 1),
    ('EXTENDED', 'Przegląd rozszerzony', 'Rozszerzony przegląd z czyszczeniem i wymianą podstawowych części', 350.00, true, 2),
    ('FULL', 'Przegląd pełny', 'Kompleksowy przegląd i konserwacja całego roweru', 600.00, true, 3);

-- Add features to Basic package
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

-- Add features to Extended package
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

-- Add features to Full package
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