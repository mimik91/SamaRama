 CREATE TABLE bike_service (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY,
    address VARCHAR(255),
    postal_code VARCHAR(10) NOT NULL,
    city VARCHAR(20) NOT NULL,
    phone_number VARCHAR(9),
    email VARCHAR(40),
    description VARCHAR(255),
    openinh_hours VARCHAR(255),
    createdat DATETIME NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (owner) REFERENCES user(id)
);