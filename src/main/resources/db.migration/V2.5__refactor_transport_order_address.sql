
ALTER TABLE transport_orders
ADD COLUMN pickup_street VARCHAR(255),
ADD COLUMN pickup_building VARCHAR(20),
ADD COLUMN pickup_apartment VARCHAR(20),
ADD COLUMN pickup_city VARCHAR(100),
ADD COLUMN pickup_postal_code VARCHAR(10);

ALTER TABLE transport_orders
ADD COLUMN delivery_street VARCHAR(255),
ADD COLUMN delivery_building VARCHAR(20),
ADD COLUMN delivery_apartment VARCHAR(20),
ADD COLUMN delivery_city VARCHAR(100),
ADD COLUMN delivery_postal_code VARCHAR(10);

ALTER TABLE transport_orders DROP COLUMN pickup_address;
ALTER TABLE transport_orders DROP COLUMN delivery_address;