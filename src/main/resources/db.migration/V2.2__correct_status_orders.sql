ALTER TABLE transport_orders
DROP CONSTRAINT IF EXISTS transport_orders_status_check;

ALTER TABLE transport_orders
ADD CONSTRAINT transport_orders_status_check
CHECK (
  status IN (
    'PENDING',
    'CONFIRMED',
    'PICKED_UP',
    'IN_SERVICE',
    'ON_THE_WAY_BACK',
    'FINISHED',
    'CANCELLED'
  )
);

DELETE FROM bicycle_enumeration_values
WHERE value IN ('COMPLETED', 'DELIVERED')
AND enumeration_id = (
  SELECT id FROM bicycle_enumerations WHERE type = 'ORDER_STATUS'
);