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

    // === INFORMACJE O ODBIORZE - ROZBITY ADRES ===

    @Column(name = "pickup_date", nullable = false)
    private LocalDate pickupDate;

    @Column(name = "pickup_street", nullable = false)
    private String pickupStreet;

    @Column(name = "pickup_building", nullable = false)
    private String pickupBuilding;

    @Column(name = "pickup_apartment")
    private String pickupApartment;

    @Column(name = "pickup_city", nullable = false)
    private String pickupCity;

    @Column(name = "pickup_postal_code")
    private String pickupPostalCode;

    @Column(name = "pickup_latitude")
    private Double pickupLatitude;

    @Column(name = "pickup_longitude")
    private Double pickupLongitude;

    @Column(name = "pickup_time_from")
    private LocalTime pickupTimeFrom;

    @Column(name = "pickup_time_to")
    private LocalTime pickupTimeTo;

    // === INFORMACJE O DOSTAWIE - ROZBITY ADRES ===

    @Column(name = "delivery_street", nullable = false)
    private String deliveryStreet;

    @Column(name = "delivery_building", nullable = false)
    private String deliveryBuilding;

    @Column(name = "delivery_apartment")
    private String deliveryApartment;

    @Column(name = "delivery_city", nullable = false)
    private String deliveryCity;

    @Column(name = "delivery_postal_code")
    private String deliveryPostalCode;

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

    // === NOWE METODY DLA ROZBITEGO ADRESU ===

    /**
     * Zwraca pełny adres odbioru jako jeden string
     */
    public String getFullPickupAddress() {
        StringBuilder address = new StringBuilder();

        if (pickupStreet != null) {
            address.append(pickupStreet);
        }

        if (pickupBuilding != null) {
            if (address.length() > 0) address.append(" ");
            address.append(pickupBuilding);
        }

        if (pickupApartment != null && !pickupApartment.trim().isEmpty()) {
            address.append("/").append(pickupApartment);
        }

        if (pickupCity != null) {
            if (address.length() > 0) address.append(", ");
            address.append(pickupCity);
        }

        if (pickupPostalCode != null && !pickupPostalCode.trim().isEmpty()) {
            if (address.length() > 0) address.append(" ");
            address.append(pickupPostalCode);
        }

        return address.toString();
    }

    /**
     * Zwraca pełny adres dostawy jako jeden string
     */
    public String getFullDeliveryAddress() {
        StringBuilder address = new StringBuilder();

        if (deliveryStreet != null) {
            address.append(deliveryStreet);
        }

        if (deliveryBuilding != null) {
            if (address.length() > 0) address.append(" ");
            address.append(deliveryBuilding);
        }

        if (deliveryApartment != null && !deliveryApartment.trim().isEmpty()) {
            address.append("/").append(deliveryApartment);
        }

        if (deliveryCity != null) {
            if (address.length() > 0) address.append(", ");
            address.append(deliveryCity);
        }

        if (deliveryPostalCode != null && !deliveryPostalCode.trim().isEmpty()) {
            if (address.length() > 0) address.append(" ");
            address.append(deliveryPostalCode);
        }

        return address.toString();
    }

    /**
     * Ustawia adres odbioru z obiektu Address lub rozbitych pól
     */
    public void setPickupAddressFromComponents(String street, String building, String apartment, String city, String postalCode) {
        this.pickupStreet = street;
        this.pickupBuilding = building;
        this.pickupApartment = apartment;
        this.pickupCity = city;
        this.pickupPostalCode = postalCode;
    }

    /**
     * Ustawia adres dostawy z obiektu Address lub rozbitych pól
     */
    public void setDeliveryAddressFromComponents(String street, String building, String apartment, String city, String postalCode) {
        this.deliveryStreet = street;
        this.deliveryBuilding = building;
        this.deliveryApartment = apartment;
        this.deliveryCity = city;
        this.deliveryPostalCode = postalCode;
    }

    /**
     * Ustawia adres dostawy z BikeService
     */
    public void setDeliveryAddressFromService(BikeService service) {
        this.deliveryStreet = service.getStreet();
        this.deliveryBuilding = service.getBuilding();
        this.deliveryApartment = service.getFlat();
        this.deliveryCity = service.getCity();
        this.deliveryPostalCode = service.getPostalCode();
        this.deliveryLatitude = service.getLatitude();
        this.deliveryLongitude = service.getLongitude();

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
        if (deliveryStreet == null && targetService != null) {
            setDeliveryAddressFromService(targetService);
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

        setDeliveryAddressFromService(targetService);

        this.status = OrderStatus.PENDING;
        this.orderDate = LocalDateTime.now();
    }

    /**
     * Konstruktor z rozbitym adresem odbioru
     */
    public TransportOrder(IncompleteBike bicycle, IncompleteUser client,
                          String pickupStreet, String pickupBuilding, String pickupApartment,
                          String pickupCity, String pickupPostalCode,
                          BikeService targetService, BigDecimal transportPrice) {
        this(bicycle, client, targetService, transportPrice);
        setPickupAddressFromComponents(pickupStreet, pickupBuilding, pickupApartment, pickupCity, pickupPostalCode);
    }
}