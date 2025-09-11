-- Tabela kategorii cennika
CREATE TABLE pricelist_categories (
    id BIGSERIAL PRIMARY KEY,
    bike_service_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    display_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (bike_service_id) REFERENCES bike_services(id) ON DELETE CASCADE
);

-- Trigger dla updated_at w pricelist_categories
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_pricelist_categories_updated_at
    BEFORE UPDATE ON pricelist_categories
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Tabela pozycji cennika
CREATE TABLE pricelist_items (
    id BIGSERIAL PRIMARY KEY,
    category_id BIGINT NOT NULL,
    service_name VARCHAR(200) NOT NULL,
    price VARCHAR(10) NOT NULL,
    description TEXT,
    display_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (category_id) REFERENCES pricelist_categories(id) ON DELETE CASCADE
);

-- Trigger dla updated_at w pricelist_items
CREATE TRIGGER update_pricelist_items_updated_at
    BEFORE UPDATE ON pricelist_items
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Tabela bike_services_registered (rozszerzenie BikeService)
CREATE TABLE bike_services_registered (
    id BIGINT PRIMARY KEY,
    website VARCHAR(255),
    description VARCHAR(1500),
    opening_hours_id BIGINT,

    FOREIGN KEY (id) REFERENCES bike_services(id) ON DELETE CASCADE
);

CREATE INDEX idx_pricelist_categories_bike_service_id ON pricelist_categories(bike_service_id);
CREATE INDEX idx_pricelist_categories_display_order ON pricelist_categories(display_order);
CREATE INDEX idx_pricelist_items_category_id ON pricelist_items(category_id);
CREATE INDEX idx_pricelist_items_display_order ON pricelist_items(display_order);
