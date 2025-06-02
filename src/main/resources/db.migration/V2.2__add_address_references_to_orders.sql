
ALTER TABLE transport_orders
ADD COLUMN pickup_address_id BIGINT REFERENCES addresses(id);

