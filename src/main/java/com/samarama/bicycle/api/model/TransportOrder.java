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
 * Rozszerza Order o funkcjonalność transportową
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
        DELIVERED_TO_SERVICE, // Dostarczono do serwisu
        PICKED_UP_FROM_SERVICE, // Odebrano z serwisu
        COMPLETED             // Zakończono
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

    @Column(name = "transport_price", nullable = false)
    private BigDecimal transportPrice = BigDecimal.ZERO;

    // === SERWIS (jeśli transport zawiera serwis) ===

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_package_id")
    private ServicePackage servicePackage;

    @Column(name = "service_package_code")
    private String servicePackageCode;

    @Column(name = "service_price")
    private BigDecimal servicePrice = BigDecimal.ZERO;

    @Column(name = "service_notes", length = 500)
    private String serviceNotes;

    // === IMPLEMENTACJA METOD ABSTRAKCYJNYCH ===

    @Override
    public String getOrderType() {
        return "TRANSPORT";
    }

    // === METODY POMOCNICZE ===

    /**
     * Zwraca łączną cenę (serwis + transport)
     */
    public BigDecimal getTotalPrice() {
        BigDecimal service = servicePrice != null ? servicePrice : BigDecimal.ZERO;
        BigDecimal transport = transportPrice != null ? transportPrice : BigDecimal.ZERO;
        return service.add(transport);
    }

    /**
     * Sprawdza czy transport jest zakończony
     */
    public boolean isTransportCompleted() {
        return transportStatus == TransportStatus.COMPLETED ||
                transportStatus == TransportStatus.DELIVERED_TO_SERVICE;
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

    /**
     * Sprawdza czy zamówienie ma przypisany pakiet serwisowy
     */
    public boolean hasServicePackage() {
        return servicePackage != null ||
                (servicePackageCode != null && !servicePackageCode.trim().isEmpty());
    }

    /**
     * Zwraca nazwę pakietu serwisowego
     */
    public String getServicePackageName() {
        return servicePackage != null ? servicePackage.getName() : servicePackageCode;
    }

    /**
     * Sprawdza czy serwis może zostać rozpoczęty
     */
    public boolean canStartService() {
        return includesService() &&
                (transportStatus == TransportStatus.DELIVERED_TO_SERVICE ||
                        getStatus() == OrderStatus.PICKED_UP ||
                        getStatus() == OrderStatus.CONFIRMED);
    }

    /**
     * Sprawdza czy serwis jest w trakcie realizacji
     */
    public boolean isInService() {
        return getStatus() == OrderStatus.IN_SERVICE;
    }

    // === KONSTRUKTORY ===

    public TransportOrder(IncompleteBike bicycle, IncompleteUser client,
                          BikeService targetService, BigDecimal transportPrice,
                          TransportType transportType) {
        super();
        setBicycle(bicycle);
        setClient(client);
        this.targetService = targetService;
        this.transportPrice = transportPrice;
        this.transportType = transportType;
        this.transportStatus = TransportStatus.PENDING;

        // Ustaw całkowitą cenę
        setPrice(getTotalPrice());

        // Ustaw adres dostawy
        this.deliveryAddress = targetService.getFullAddress();
        this.deliveryLatitude = targetService.getLatitude();
        this.deliveryLongitude = targetService.getLongitude();
    }

    public TransportOrder(IncompleteBike bicycle, IncompleteUser client,
                          BikeService targetService, ServicePackage servicePackage,
                          BigDecimal transportPrice) {
        this(bicycle, client, targetService, transportPrice, TransportType.SERVICE_WITH_TRANSPORT);

        this.servicePackage = servicePackage;
        this.servicePackageCode = servicePackage.getCode();
        this.servicePrice = servicePackage.getPrice();

        // Zaktualizuj całkowitą cenę
        setPrice(getTotalPrice());
    }

    // === LIFECYCLE HOOKS ===

    @PreUpdate
    protected void onUpdate() {
        super.onUpdate();
        // Automatycznie aktualizuj całkowitą cenę
        if (getPrice() == null || !getPrice().equals(getTotalPrice())) {
            setPrice(getTotalPrice());
        }
    }
}