-- Delete any enumeration records for SERVICE_PACKAGE type
DELETE FROM bicycle_enumeration_values
WHERE enumeration_id IN (
    SELECT id
    FROM bicycle_enumerations
    WHERE type = 'SERVICE_PACKAGE'
);

-- Delete the SERVICE_PACKAGE enumeration type itself
DELETE FROM bicycle_enumerations
WHERE type = 'SERVICE_PACKAGE';

-- Delete any SERVICE_PACKAGE_BASIC, SERVICE_PACKAGE_EXTENDED, SERVICE_PACKAGE_FULL enumerations
DELETE FROM bicycle_enumeration_values
WHERE enumeration_id IN (
    SELECT id
    FROM bicycle_enumerations
    WHERE type LIKE 'SERVICE_PACKAGE_%'
);

DELETE FROM bicycle_enumerations
WHERE type LIKE 'SERVICE_PACKAGE_%';

-- Delete any SERVICE_PACKAGE_PRICES enumeration type and values
DELETE FROM bicycle_enumeration_values
WHERE enumeration_id IN (
    SELECT id
    FROM bicycle_enumerations
    WHERE type = 'SERVICE_PACKAGE_PRICES'
);

DELETE FROM bicycle_enumerations
WHERE type = 'SERVICE_PACKAGE_PRICES';