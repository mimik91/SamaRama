CREATE TABLE service_users (
    id BIGINT PRIMARY KEY,
    service_name VARCHAR(100) NOT NULL,
    bike_service_id BIGINT NOT NULL,
    password VARCHAR(120) NOT NULL,
    verified BOOLEAN DEFAULT FALSE,

    CONSTRAINT fk_service_users_id
        FOREIGN KEY (id)
        REFERENCES incomplete_users(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_service_users_bike_service_id
        FOREIGN KEY (bike_service_id)
        REFERENCES bike_services(id)
        ON DELETE CASCADE
);