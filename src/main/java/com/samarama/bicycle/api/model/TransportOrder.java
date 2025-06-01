package com.samarama.bicycle.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * TransportOrder - TYLKO transport roweru do zewnętrznego serwisu
 * (ServiceOrder obsługuje transport + serwis)
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@Entity
@Table(name = "transport_orders")
@PrimaryKeyJoinColumn(name = "id")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class TransportOrder extends Order {

    public enum TransportStatus {
        PENDING,               // Oczekuje na odbiór
        PICKED_UP,            // Odebrano od klienta
        IN_TRANSIT,           // W transporcie
        DELIVERED,            // Dostarczono do serwisu
        COMPLETED             // Zakończono
    }

    // === INFORMACJE O TRANSPORCIE ===

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_service_id", nullable = false)
    private BikeService targetService; // zewnętrzny serwis

    @Column(name = "delivery_address")
    private String deliveryAddress;

    @Column(name = "delivery_latitude")
    private Double deliveryLatitude;

    @Column(name = "delivery_longitude")
    private Double deliveryLongitude;

    // === CZAS I STATUS TRANSPORTU ===

    @Column(name = "pickup_time_from")
    private LocalTime pickupTimeFrom;

    @Column(name = "pickup_time_to")
    private LocalTime pickupTimeTo;

    @Enumerated(EnumType.STRING)
    @Column(name = "transport_status", nullable = false)
    private TransportStatus transportStatus = TransportStatus.PENDING;

    @Column(name = "estimated_delivery_time")
    private LocalDateTime estimatedDeliveryTime;

    @Column(name = "actual_delivery_time")
    private LocalDateTime actualDeliveryTime;

    // === INFORMACJE DODATKOWE ===

    @Column(name = "transport_notes", length = 500)
    private String transportNotes;

    @Column(name = "estimated_time")
    private Integer estimatedTime; // w minutach

    // === IMPLEMENTACJA METOD ABSTRAKCYJNYCH ===

    @Override
    public String getOrderType() {
        return "TRANSPORT";
    }

    // === METODY POMOCNICZE ===

    /**
     * Sprawdza czy transport jest zakończony
     */
    public boolean isTransportCompleted() {
        return transportStatus == TransportStatus.COMPLETED ||
                transportStatus == TransportStatus.DELIVERED;
    }

    /**
     * Zwraca adres dostawy
     */
    public String getDeliveryAddress() {
        if (deliveryAddress != null && !deliveryAddress.trim().isEmpty()) {
            return deliveryAddress;
        }
        return targetService != null ? targetService.getFullAddress() : null;
    }

    // === KONSTRUKTORY ===

    public TransportOrder(IncompleteBike bicycle, IncompleteUser client,
                          BikeService targetService, BigDecimal transportPrice) {
        super();
        setBicycle(bicycle);
        setClient(client);
        this.targetService = targetService;
        this.transportStatus = TransportStatus.PENDING;
        setPrice(transportPrice != null ? transportPrice : BigDecimal.ZERO);

        // Ustaw adres dostawy
        this.deliveryAddress = targetService.getFullAddress();
        this.deliveryLatitude = targetService.getLatitude();
        this.deliveryLongitude = targetService.getLongitude();
    }
}