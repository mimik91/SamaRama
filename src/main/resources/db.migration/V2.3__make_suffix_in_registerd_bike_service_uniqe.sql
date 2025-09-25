ALTER TABLE bike_services_registered
ADD CONSTRAINT uq_bike_services_registered_suffix UNIQUE (suffix);
