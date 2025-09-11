CREATE TABLE opening_hours (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY
);

CREATE TABLE opening_intervals (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    opening_hours_id BIGINT NOT NULL,
    day_of_week VARCHAR(10) NOT NULL,
    open_time VARCHAR(5) NOT NULL,
    close_time VARCHAR(5) NOT NULL,
    FOREIGN KEY (opening_hours_id) REFERENCES opening_hours(id)
);