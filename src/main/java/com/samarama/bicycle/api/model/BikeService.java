package com.samarama.bicycle.api.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "bike_services")
public class BikeService {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String name;

    @NotBlank
    private String street;

    @NotBlank
    private String building;

    private String flat;

    private String postalCode;

    @NotBlank
    private String city;

    private String phoneNumber;

    private String businessPhone;

    @Email
    @Column(unique = true)
    private String email;

    @NotBlank
    private String password;

    @OneToOne(mappedBy = "bikeService", cascade = CascadeType.ALL, orphanRemoval = true)
    private OpeningHours openingHours;

    private Double latitude;

    private Double longitude;

    private String description;

    private boolean verified = false;

    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToMany(mappedBy = "service", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<ServiceRecord> serviceRecords = new HashSet<>();
}