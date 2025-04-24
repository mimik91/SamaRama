-- Add CITY enumeration type
INSERT INTO bicycle_enumerations (type) VALUES ('CITY');
INSERT INTO bicycle_enumeration_values (enumeration_id, value)
SELECT id, unnest(ARRAY[
    'Kraków', 'Węgrzce', 'Zielonki', 'Bosutów', 'Bibice', 'Batowice', 'Michałowice', 'Dziekanowice', 'Raciborowice', 'Boleń'
])
FROM bicycle_enumerations WHERE type = 'CITY';