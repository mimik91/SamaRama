-- Usuwa istniejącą tabelę, jeśli istnieje.
DROP TABLE IF EXISTS bike_service_repair_coverage;

-- Tworzy tabelę od nowa z poprawnymi nazwami kolumn.
CREATE TABLE bike_service_repair_coverage (
    bike_service_id BIGINT NOT NULL,
    bike_service_coverage_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (bike_service_id, bike_service_coverage_id),
    FOREIGN KEY (bike_service_id)
        REFERENCES bike_services_registered(id) ON DELETE CASCADE,
    FOREIGN KEY (bike_service_coverage_id)
        REFERENCES bike_repair_coverage(id) ON DELETE CASCADE
);