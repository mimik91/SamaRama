INSERT INTO bike_services (
    id,
    name,
    description,
    street,
    building,
    city,
    created_at,
    updated_at
) VALUES (
    2137,
    'Serwis Domyślny',
    'Domyślny serwis rowerowy w Krakowie',
    'Kiełkowskiego',
    '10b',
    'Kraków',
    NOW(),
    NOW()
);

-- Ustaw sekwencję na wartość większą niż najwyższe ID, żeby uniknąć konfliktów
SELECT setval('bike_services_id_seq', GREATEST(2138, (SELECT MAX(id) + 1 FROM bike_services)));