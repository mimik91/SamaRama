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
    @Column(name = "id") // Nazwa kolumny
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bicycle_id") // Nazwa kolumny
    private IncompleteBike bicycle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id") // Nazwa kolumny
    private IncompleteUser client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_package_id") // Nazwa kolumny
    private ServicePackage servicePackage;

    // Zachowanie kompatybilności wstecznej - kolumna przechowująca kod pakietu
    @Column(name = "service_package_code") // Nazwa kolumny
    private String servicePackageCode;

    @Column(name = "pickup_date", nullable = false) // Nazwa kolumny
    private LocalDate pickupDate;

    @Column(name = "pickup_address") // Nazwa kolumny
    private String pickupAddress;

    @Column(name = "pickup_latitude") // Nazwa kolumny
    private Double pickupLatitude;

    @Column(name = "pickup_longitude") // Nazwa kolumny
    private Double pickupLongitude;

    @Column(name = "price", nullable = false) // Nazwa kolumny
    private BigDecimal price;

    @Column(name = "order_date", nullable = false) // Nazwa kolumny
    private LocalDateTime orderDate = LocalDateTime.now();

    @Column(name = "additional_notes", length = 500) // Nazwa kolumny
    private String additionalNotes;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false) // Nazwa kolumny
    private OrderStatus status = OrderStatus.PENDING;

    // Opcjonalne uwagi od serwisu
    @Column(name = "service_notes") // Nazwa kolumny
    private String serviceNotes;
}