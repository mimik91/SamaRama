CREATE TABLE bicycle (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY,
    frameNumber VARCHAR(20) NOT NULL,
    brand VARCHAR(20) NOT NULL,
    model VARCHAR(40),
    username VARCHAR(40),
    owner INT,
    createdat DATETIME NOT NULL,
    production_date DATE,
    PRIMARY KEY (id),
    FOREIGN KEY (owner) REFERENCES user(id)
);