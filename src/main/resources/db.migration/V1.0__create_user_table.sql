CREATE TABLE users (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY,
    email VARCHAR(50) NOT NULL,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    phone_number VARCHAR(15),
    password VARCHAR(120) NOT NULL,
    role VARCHAR(20) DEFAULT 'CLIENT',
    verified BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL,
    PRIMARY KEY (id),
    UNIQUE (email)
);