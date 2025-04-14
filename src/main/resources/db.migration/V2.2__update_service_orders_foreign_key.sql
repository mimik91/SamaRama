ALTER TABLE service_orders DROP CONSTRAINT IF EXISTS fkqryiu8hq62083ky1cg6i8bol3;

ALTER TABLE service_orders
    ADD CONSTRAINT fk_service_orders_incomplete_bike
    FOREIGN KEY (bicycle_id)
    REFERENCES incomplete_bikes(id);