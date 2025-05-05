package com.samarama.bicycle.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "service_records")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ServiceRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id") // Nazwa kolumny
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bicycle_id") // Nazwa kolumny
    @JsonIgnoreProperties({"owner", "serviceRecords", "photo"})
    private Bicycle bicycle;

    @NotBlank
    @Column(name = "name") // Nazwa kolumny
    private String name;

    @NotBlank
    @Column(name = "description") // Nazwa kolumny
    private String description;

    @NotNull
    @Column(name = "service_date") // Nazwa kolumny
    private LocalDate serviceDate;

    @Column(name = "price") // Nazwa kolumny
    private BigDecimal price;
}