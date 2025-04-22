CREATE TABLE bicycles (
    id BIGINT NOT NULL,
    frame_number VARCHAR(20),
    PRIMARY KEY (id),
    UNIQUE (frame_number),
    FOREIGN KEY (id) REFERENCES incomplete_bikes(id) ON DELETE CASCADE
);