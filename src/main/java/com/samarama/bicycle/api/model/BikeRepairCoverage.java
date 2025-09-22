package com.samarama.bicycle.api.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

import java.util.HashSet;
import java.util.Set;

@Data
@Entity
@Table(name = "bike_repair_coverage")
@RequiredArgsConstructor
public class BikeRepairCoverage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private BikeRepairCoverageCategory category;

    @ManyToMany(mappedBy = "bikeRepairCoverages", fetch = FetchType.LAZY)
    private Set<BikeServiceRegistered> bikeServices = new HashSet<>();
}