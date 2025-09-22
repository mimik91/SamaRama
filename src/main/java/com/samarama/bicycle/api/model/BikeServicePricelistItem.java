package com.samarama.bicycle.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Tabela łącząca serwis z pozycjami cennika
 * Przechowuje ceny i kolejność wyświetlania dla każdego serwisu osobno
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "bike_service_pricelist_items",
        uniqueConstraints = @UniqueConstraint(columnNames = {"bike_service_id", "pricelist_item_id"}))
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class BikeServicePricelistItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /**
     * ID serwisu
     */
    @NotNull
    @Column(name = "bike_service_id", nullable = false)
    private Long bikeServiceId;

    /**
     * ID pozycji cennika
     */
    @NotNull
    @Column(name = "pricelist_item_id", nullable = false)
    private Long pricelistItemId;

    /**
     * Serwis do którego należy ta pozycja cennika
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bike_service_id", insertable = false, updatable = false)
    private BikeServiceRegistered bikeService;

    /**
     * Pozycja cennika
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pricelist_item_id", insertable = false, updatable = false)
    private PricelistItem pricelistItem;

    /**
     * Cena usługi w tym serwisie (np. "50zł", "od 30zł", "50-80zł")
     * Jeśli null, używana jest basePrice z PricelistItem
     */
    @Size(max = 100)
    @Column(name = "price")
    private String price;

    /**
     * Kolejność wyświetlania w ramach kategorii dla tego serwisu
     */
    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    /**
     * Dodatkowe informacje specyficzne dla tego serwisu
     */
    @Size(max = 500)
    @Column(name = "service_specific_note")
    private String serviceSpecificNote;

    /**
     * Czy usługa jest aktywna w tym serwisie
     */
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        this.updatedAt = LocalDateTime.now();
    }

    // === KONSTRUKTORY POMOCNICZE ===

    public BikeServicePricelistItem(Long bikeServiceId, Long pricelistItemId, String price) {
        this.bikeServiceId = bikeServiceId;
        this.pricelistItemId = pricelistItemId;
        this.price = price;
    }

    public BikeServicePricelistItem(Long bikeServiceId, Long pricelistItemId, String price, Integer displayOrder) {
        this(bikeServiceId, pricelistItemId, price);
        this.displayOrder = displayOrder;
    }
}