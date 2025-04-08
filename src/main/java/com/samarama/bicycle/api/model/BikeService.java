package com.samarama.bicycle.api.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
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
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
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
    @JsonIgnore // Ignoruj hasło podczas serializacji
    private String password;

    @OneToOne(mappedBy = "bikeService", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference // To rozwiązuje problem nieskończonej rekurencji
    private OpeningHours openingHours;

    private Double latitude;

    private Double longitude;

    private String description;

    private boolean verified = false;

    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToMany(mappedBy = "service", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore // Ignorujemy serwisowe rekordy w zwykłym wyniku API
    private Set<ServiceRecord> serviceRecords = new HashSet<>();
}