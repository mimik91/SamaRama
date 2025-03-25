 CREATE TABLE service_record (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY,
    bicycle_id INT NOT NULL,
    name VARCHAR(20) NOT NULL,
    description VARCHAR(255) NOT NULL,
    service_id INT NOT NULL,
    price INT,
    service_date DATE NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (owner) REFERENCES user(id)
);

    @NotNull
    private LocalDate serviceDate;

    private BigDecimal price;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "serviceman_id")
    private User serviceman;