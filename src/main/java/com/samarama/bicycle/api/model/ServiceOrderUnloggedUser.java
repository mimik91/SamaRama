package com.samarama.bicycle.api.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class ServiceOrderUnloggedUser {

    public enum OrderStatus {
        PENDING, CONFIRMED, PICKED_UP, IN_SERVICE, COMPLETED, DELIVERED, CANCELLED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private String bicycleBrand;

    @Column
    private String bicycleModel;

    @NotBlank
    @Size(max = 50)
    @Email
    @Column
    private String email;

    @Size(max = 15)
    private String phoneNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_package_id")
    private ServicePackage servicePackage;

    @Column(nullable = false)
    private LocalDate pickupDate;

    private String pickupAddress;

    private Double pickupLatitude;

    private Double pickupLongitude;

    @Column(nullable = false)
    private BigDecimal price;

    @Column(nullable = false)
    private LocalDateTime orderDate = LocalDateTime.now();

    @Column(length = 500)
    private String additionalNotes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ServiceOrder.OrderStatus status = ServiceOrder.OrderStatus.PENDING;

    // Opcjonalne uwagi od serwisu
    private String serviceNotes;
}
