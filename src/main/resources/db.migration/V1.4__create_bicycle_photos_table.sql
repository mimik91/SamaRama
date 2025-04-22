CREATE TABLE service_package_features (
    package_id BIGINT NOT NULL,
    feature VARCHAR(500) NOT NULL,
    PRIMARY KEY (package_id, feature),
    FOREIGN KEY (package_id) REFERENCES service_packages(id) ON DELETE CASCADE
);

CREATE INDEX idx_service_packages_code ON service_packages(code);
CREATE INDEX idx_service_packages_active ON service_packages(active);