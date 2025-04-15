-- Create service packages table
CREATE TABLE service_packages (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY,
    code VARCHAR(20) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    price DECIMAL(10,2) NOT NULL,
    active BOOLEAN DEFAULT TRUE,
    display_order INTEGER,
    PRIMARY KEY (id)
);

-- Create service package features table
CREATE TABLE service_package_features (
    package_id BIGINT NOT NULL,
    feature VARCHAR(500) NOT NULL,
    PRIMARY KEY (package_id, feature),
    FOREIGN KEY (package_id) REFERENCES service_packages(id) ON DELETE CASCADE
);

-- Create indexes
CREATE INDEX idx_service_packages_code ON service_packages(code);
CREATE INDEX idx_service_packages_active ON service_packages(active);