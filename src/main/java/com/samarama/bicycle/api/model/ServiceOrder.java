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

    public enum OrderStatus {
        PENDING, CONFIRMED, PICKED_UP, IN_SERVICE, COMPLETED, DELIVERED, CANCELLED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bicycle_id")
    private IncompleteBike bicycle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private IncompleteUser client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_package_id")
    private ServicePackage servicePackage;

    // Zachowanie kompatybilności wstecznej - kolumna przechowująca kod pakietu
    @Column(name = "service_package_code")
    private String servicePackageCode;

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
}