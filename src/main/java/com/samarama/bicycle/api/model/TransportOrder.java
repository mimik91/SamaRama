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
 * TransportOrder - zamówienie transportu roweru do serwisu
 * Rozszerza ServiceOrder o funkcjonalność transportową
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@Entity
@Table(name = "transport_orders")
@PrimaryKeyJoinColumn(name = "id")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class TransportOrder extends ServiceOrder {

    public enum TransportStatus {
        PENDING,               // Oczekuje na odbiór
        PICKED_UP,            // Odebrano od klienta
        IN_TRANSIT,           // W transporcie
        DELIVERED_TO_SERVICE,
        PICKED_UP_FROM_SERVICE,
        COMPLETED
    }

    public enum TransportType {
        TO_SERVICE_ONLY,       // Tylko transport do serwisu
        SERVICE_WITH_TRANSPORT // Serwis + transport
    }

    // === INFORMACJE O TRANSPORCIE ===

    @Enumerated(EnumType.STRING)
    @Column(name = "transport_type", nullable = false)
    private TransportType transportType = TransportType.TO_SERVICE_ONLY;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_service_id", nullable = false)
    private BikeService targetService;



    // === CZAS I STATUS TRANSPORTU ===

    @Column(name = "pickup_time_from")
    private LocalTime pickupTimeFrom;

    @Column(name = "pickup_time_to")
    private LocalTime pickupTimeTo;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TransportStatus status = TransportStatus.PENDING;

    @Column(name = "estimated_delivery_time")
    private LocalDateTime estimatedDeliveryTime;

    @Column(name = "actual_delivery_time")
    private LocalDateTime actualDeliveryTime;

    // === INFORMACJE DODATKOWE ===

    @Column(name = "notes", length = 500)
    private String notes;

    @Column(name = "estimated_time")
    private Integer estimatedTime; // w minutach

    @Column(name = "price", nullable = false)
    private BigDecimal price;

    // === METODY POMOCNICZE ===

    /**
     * Zwraca łączną cenę (serwis + transport)
     */
    public BigDecimal getTotalPrice() {
        BigDecimal servicePrice = super.getPrice() != null ? super.getPrice() : BigDecimal.ZERO;
        BigDecimal transportCost = price != null ? price : BigDecimal.ZERO;
        return servicePrice.add(transportCost);
    }

    /**
     * Sprawdza czy transport jest zakończony
     */
    public boolean isCompleted() {
        return status == TransportStatus.COMPLETED ||
                status == TransportStatus.DELIVERED_TO_SERVICE;
    }

    /**
     * Zwraca adres dostawy (zawsze adres serwisu)
     */
    public String getDeliveryAddress() {
        return targetService != null ? targetService.getFullAddress() : null;
    }

    /**
     * Sprawdza czy zamówienie zawiera także serwis
     */
    public boolean includesService() {
        return transportType == TransportType.SERVICE_WITH_TRANSPORT;
    }

    /**
     * Sprawdza czy to tylko transport bez serwisu
     */
    public boolean isTransportOnly() {
        return transportType == TransportType.TO_SERVICE_ONLY;
    }

    // === KONSTRUKTORY ===

    public TransportOrder(ServiceOrder serviceOrder, BikeService targetService,
                          BigDecimal transportPrice, TransportType transportType) {
        // Kopiuj wszystkie pola z ServiceOrder
        super.setId(serviceOrder.getId());
        super.setBicycle(serviceOrder.getBicycle());
        super.setClient(serviceOrder.getClient());
        super.setServicePackage(serviceOrder.getServicePackage());
        super.setServicePackageCode(serviceOrder.getServicePackageCode());
        super.setPickupDate(serviceOrder.getPickupDate());
        super.setPickupAddress(serviceOrder.getPickupAddress());
        super.setPickupLatitude(serviceOrder.getPickupLatitude());
        super.setPickupLongitude(serviceOrder.getPickupLongitude());
        super.setPrice(serviceOrder.getPrice());
        super.setAdditionalNotes(serviceOrder.getAdditionalNotes());
        super.setStatus(serviceOrder.getStatus());
        super.setOrderDate(serviceOrder.getOrderDate());

        // Ustaw pola transportowe
        this.targetService = targetService;
        this.price = transportPrice;
        this.transportType = transportType;
        this.status = TransportStatus.PENDING;
    }
}