package com.samarama.bicycle.api.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
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
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Set<BikeServiceRegistered> bikeServices = new HashSet<>();

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}