-- Modify the incomplete_bikes table to be a parent class
ALTER TABLE incomplete_bikes ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT NOW();
ALTER TABLE incomplete_bikes ADD COLUMN production_date DATE;
ALTER TABLE incomplete_bikes ADD COLUMN owner_id BIGINT;
ALTER TABLE incomplete_bikes ADD CONSTRAINT fk_owner FOREIGN KEY (owner_id) REFERENCES users(id);

-- Modify bicycles table to inherit from incomplete_bikes
ALTER TABLE bicycles DROP COLUMN brand;
ALTER TABLE bicycles DROP COLUMN model;
ALTER TABLE bicycles DROP COLUMN type;
ALTER TABLE bicycles DROP COLUMN frame_material;
ALTER TABLE bicycles DROP COLUMN owner_id;
ALTER TABLE bicycles DROP COLUMN created_at;
ALTER TABLE bicycles DROP COLUMN production_date;
ALTER TABLE bicycles DROP COLUMN photo;

-- Add a foreign key to bicycles pointing to incomplete_bikes
ALTER TABLE bicycles ADD CONSTRAINT fk_incomplete_bike FOREIGN KEY (id) REFERENCES incomplete_bikes(id) ON DELETE CASCADE;

-- Modify bicycle_photos table to reference bike_id
ALTER TABLE bicycle_photos DROP COLUMN bicycle_id;
ALTER TABLE bicycle_photos DROP COLUMN incomplete_bike_id;
ALTER TABLE bicycle_photos ADD COLUMN bike_id BIGINT;
ALTER TABLE bicycle_photos ADD CONSTRAINT fk_bike FOREIGN KEY (bike_id) REFERENCES incomplete_bikes(id) ON DELETE CASCADE;
ALTER TABLE bicycle_photos RENAME COLUMN photo_data TO photo_data;