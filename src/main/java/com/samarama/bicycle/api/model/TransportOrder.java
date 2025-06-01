package com.samarama.bicycle.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * TransportOrder - bazowa klasa dla wszystkich zamówień transportu rowerów
 * Może być to:
 * - Czysty transport do zewnętrznego serwisu
 * - Transport + serwis (ServiceOrder extends TransportOrder)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "transport_orders")
@Inheritance(strategy = InheritanceType.JOINED)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class TransportOrder {

    public enum OrderStatus {
        PENDING,         // Oczekuje na potwierdzenie
        CONFIRMED,       // Potwierdzone - gotowe do odbioru
        PICKED_UP,       // Odebrano od klienta
        IN_SERVICE,      // W serwisie (tylko dla ServiceOrder)
        ON_THE_WAY_BACK, // W drodze powrotnej do klienta
        CANCELLED        // Anulowane
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    // === PODSTAWOWE INFORMACJE O ZAMÓWIENIU ===

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bicycle_id", nullable = false)
    private IncompleteBike bicycle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private IncompleteUser client;

    // === INFORMACJE O ODBIORZE ===

    @Column(name = "pickup_date", nullable = false)
    private LocalDate pickupDate;

    @Column(name = "pickup_address", nullable = false)
    private String pickupAddress;

    @Column(name = "pickup_latitude")
    private Double pickupLatitude;

    @Column(name = "pickup_longitude")
    private Double pickupLongitude;

    @Column(name = "pickup_time_from")
    private LocalTime pickupTimeFrom;

    @Column(name = "pickup_time_to")
    private LocalTime pickupTimeTo;

    // === INFORMACJE O DOSTAWIE ===

    @Column(name = "delivery_address", nullable = false)
    private String deliveryAddress;

    @Column(name = "delivery_latitude")
    private Double deliveryLatitude;

    @Column(name = "delivery_longitude")
    private Double deliveryLongitude;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_service_id", nullable = false)
    private BikeService targetService; // może być serwis własny lub zewnętrzny

    // === STATUS I DATY ===

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private OrderStatus status = OrderStatus.PENDING;

    @Column(name = "order_date", nullable = false)
    private LocalDateTime orderDate = LocalDateTime.now();

    // === CENY ===

    @Column(name = "transport_price", nullable = false)
    private BigDecimal transportPrice; // TYLKO koszt transportu

    // === TRANSPORT DETAILS ===

    @Column(name = "estimated_time")
    private Integer estimatedTime; // w minutach

    @Column(name = "actual_pickup_time")
    private LocalDateTime actualPickupTime;

    @Column(name = "actual_delivery_time")
    private LocalDateTime actualDeliveryTime;

    // === NOTATKI ===

    @Column(name = "transport_notes", length = 500)
    private String transportNotes;

    @Column(name = "additional_notes", length = 500)
    private String additionalNotes;

    // === METADANE ===

    @Column(name = "last_modified_by")
    private String lastModifiedBy;

    @Column(name = "last_modified_date")
    private LocalDateTime lastModifiedDate;

    // === METODY ABSTRAKCYJNE (do nadpisania w ServiceOrder) ===

    /**
     * Zwraca typ zamówienia - do nadpisania w ServiceOrder
     */
    public String getOrderType() {
        return "TRANSPORT";
    }

    /**
     * Zwraca całkowitą cenę zamówienia - do nadpisania w ServiceOrder
     */
    public BigDecimal getTotalPrice() {
        return transportPrice;
    }

    /**
     * Czy to jest zamówienie serwisowe - do nadpisania w ServiceOrder
     */
    public boolean isServiceOrder() {
        return false;
    }

    // === METODY BIZNESOWE ===

    /**
     * Sprawdza czy zamówienie można anulować
     */
    public boolean canBeCancelled() {
        return status == OrderStatus.PENDING || status == OrderStatus.CONFIRMED;
    }

    /**
     * Sprawdza czy zamówienie jest aktywne (nieanulowane)
     */
    public boolean isActive() {
        return status != OrderStatus.CANCELLED;
    }

    /**
     * Sprawdza czy zamówienie jest zakończone
     */
    public boolean isCompleted() {
        return status == OrderStatus.ON_THE_WAY_BACK;
    }

    /**
     * Sprawdza czy rower jest obecnie w serwisie
     */
    public boolean isInService() {
        return status == OrderStatus.IN_SERVICE;
    }

    /**
     * Sprawdza czy rower został odebrany od klienta
     */
    public boolean isPickedUp() {
        return status == OrderStatus.PICKED_UP || status == OrderStatus.IN_SERVICE || status == OrderStatus.ON_THE_WAY_BACK;
    }

    /**
     * Sprawdza czy to jest transport do serwisu własnego
     */
    public boolean isToOwnService() {
        return targetService != null && targetService.getId().equals(1L); // zakładamy że ID 1 = serwis własny
    }

    /**
     * Sprawdza czy to jest transport do serwisu zewnętrznego
     */
    public boolean isToExternalService() {
        return !isToOwnService();
    }

    /**
     * Zwraca czytelną nazwę statusu
     */
    public String getStatusDisplayName() {
        return switch (status) {
            case PENDING -> "Oczekujące";
            case CONFIRMED -> "Potwierdzone";
            case PICKED_UP -> "Odebrane";
            case IN_SERVICE -> "W serwisie";
            case ON_THE_WAY_BACK -> "W drodze powrotnej";
            case CANCELLED -> "Anulowane";
        };
    }

    /**
     * Sprawdza czy ma okno czasowe odbioru
     */
    public boolean hasPickupTimeWindow() {
        return pickupTimeFrom != null && pickupTimeTo != null;
    }

    /**
     * Zwraca okno czasowe odbioru jako string
     */
    public String getPickupTimeWindow() {
        if (hasPickupTimeWindow()) {
            return pickupTimeFrom + " - " + pickupTimeTo;
        }
        return null;
    }

    // === LIFECYCLE HOOKS ===

    @PrePersist
    protected void onCreate() {
        if (orderDate == null) {
            orderDate = LocalDateTime.now();
        }
        if (status == null) {
            status = OrderStatus.PENDING;
        }
        if (deliveryAddress == null && targetService != null) {
            deliveryAddress = targetService.getFullAddress();
            deliveryLatitude = targetService.getLatitude();
            deliveryLongitude = targetService.getLongitude();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        lastModifiedDate = LocalDateTime.now();
    }

    // === KONSTRUKTORY ===

    public TransportOrder(IncompleteBike bicycle, IncompleteUser client,
                          BikeService targetService, BigDecimal transportPrice) {
        this.bicycle = bicycle;
        this.client = client;
        this.targetService = targetService;
        this.transportPrice = transportPrice != null ? transportPrice : BigDecimal.ZERO;
        this.deliveryAddress = targetService.getFullAddress();
        this.deliveryLatitude = targetService.getLatitude();
        this.deliveryLongitude = targetService.getLongitude();
        this.status = OrderStatus.PENDING;
        this.orderDate = LocalDateTime.now();
    }
}