-- Add a sample bike service if there isn't one already
INSERT INTO bike_services (
    name,
    street,
    building,
    city,
    postal_code,
    phone_number,
    email,
    password,
    latitude,
    longitude,
    description,
    verified,
    created_at
)
SELECT
    'Rowerowy Serwis Premium',
    'Długa',
    '25',
    'Kraków',
    '30-001',
    '123456789',
    'serwis@example.com',
    '$2a$10$JQOfPxoQlKw2YGjYkN6we.p2y2Nt3s5QFUKlL6Yi9P2livRuAyLqK', -- hashed 'password123'
    50.0647,
    19.9450,
    'Profesjonalny serwis rowerowy oferujący pełen zakres usług.',
    true,
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM bike_services LIMIT 1);