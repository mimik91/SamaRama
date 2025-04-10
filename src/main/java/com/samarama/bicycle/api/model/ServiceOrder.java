package com.samarama.bicycle.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "service_orders")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ServiceOrder {

    public enum ServicePackage {
        BASIC, EXTENDED, FULL
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bicycle_id")
    private Bicycle bicycle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id")
    private BikeService service;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
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
    private OrderStatus status = OrderStatus.PENDING;

    // Opcjonalne uwagi od serwisu
    private String serviceNotes;

    public enum OrderStatus {
        PENDING, CONFIRMED, PICKED_UP, IN_SERVICE, COMPLETED, DELIVERED, CANCELLED
    }
}