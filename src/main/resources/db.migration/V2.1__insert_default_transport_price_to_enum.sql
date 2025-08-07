INSERT INTO bicycle_enumerations (type) VALUES ('DEFAULT_TRANSPORT_PRICE');
INSERT INTO bicycle_enumeration_values (enumeration_id, value)
SELECT id, unnest(ARRAY[
    '30'
])
FROM bicycle_enumerations WHERE type = 'DEFAULT_TRANSPORT_PRICE';

INSERT INTO bicycle_enumerations (type) VALUES ('DISCOUNT_CODE');
INSERT INTO bicycle_enumeration_values (enumeration_id, value)
SELECT id, unnest(ARRAY[
    'JOMI20', 'FIRST10', 'LET10'
])
FROM bicycle_enumerations WHERE type = 'DISCOUNT_CODE';