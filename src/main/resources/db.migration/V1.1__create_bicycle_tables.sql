-- V1.1__create_bicycle_tables.sql

-- Tabela bazowa dla rowerów (kompletnych i niekompletnych)
CREATE TABLE incomplete_bikes (
    id BIGSERIAL,
    brand VARCHAR(20) NOT NULL,
    model VARCHAR(40),
    type VARCHAR(40),
    frame_material VARCHAR(100),
    owner_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    production_date DATE,
    PRIMARY KEY (id),
    FOREIGN KEY (owner_id) REFERENCES incomplete_users(id)
);

-- Tabela dla kompletnych rowerów (dziedziczy po incomplete_bikes)
CREATE TABLE bicycles (
    id BIGINT NOT NULL,
    frame_number VARCHAR(20) UNIQUE,
    PRIMARY KEY (id),
    FOREIGN KEY (id) REFERENCES incomplete_bikes(id) ON DELETE CASCADE
);

-- Tabela zdjęć rowerów
CREATE TABLE bicycle_photos (
    id BIGSERIAL,
    bike_id BIGINT,
    photo_data BYTEA,
    content_type VARCHAR(100),
    file_size BIGINT,
    uploaded_at TIMESTAMP NOT NULL DEFAULT NOW(),
    PRIMARY KEY (id),
    FOREIGN KEY (bike_id) REFERENCES incomplete_bikes(id) ON DELETE CASCADE
);

-- Tabela enumeracji dla rowerów
CREATE TABLE bicycle_enumerations (
    id BIGSERIAL,
    type VARCHAR(50) NOT NULL UNIQUE,
    PRIMARY KEY (id)
);

-- Tabela wartości enumeracji
CREATE TABLE bicycle_enumeration_values (
    enumeration_id BIGINT NOT NULL,
    value VARCHAR(100) NOT NULL,
    PRIMARY KEY (enumeration_id, value),
    FOREIGN KEY (enumeration_id) REFERENCES bicycle_enumerations(id) ON DELETE CASCADE
);