-- Migration: V1__Create_addresses_table.sql

CREATE TABLE addresses (
    id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    street VARCHAR(255) NOT NULL,
    building_number VARCHAR(20) NOT NULL,
    apartment_number VARCHAR(20),
    city VARCHAR(100) NOT NULL,
    postal_code VARCHAR(10),
    name VARCHAR(100),
    user_id BIGINT NOT NULL,
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    transport_notes VARCHAR(500),
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);