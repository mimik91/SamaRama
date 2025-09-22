package com.samarama.bicycle.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Pozycja cennika - pojedyncza usługa z ceną
 * Może być przypisana do wielu kategorii i wielu serwisów
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "pricelist_items")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@ToString(exclude = {"categories", "bikeServicePricelistItems"})
public class PricelistItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /**
     * Nazwa usługi (np. "Wymiana łańcucha")
     */
    @NotBlank
    @Size(max = 200)
    @Column(name = "service_name", nullable = false)
    private String serviceName;

    /**
     * Bazowa cena usługi (np. "50zł", "od 30zł", "50-80zł")
     * Może być nadpisana w konkretnym serwisie przez BikeServicePricelistItem
     */
    @Size(max = 100)
    @Column(name = "base_price")
    private String basePrice;

    /**
     * Dodatkowy opis usługi
     */
    @Size(max = 1000)
    @Column(name = "description")
    private String description;

    /**
     * Kategorie do których należy pozycja
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "pricelist_item_categories",
            joinColumns = @JoinColumn(name = "pricelist_item_id"),
            inverseJoinColumns = @JoinColumn(name = "pricelist_category_id")
    )
    private Set<PricelistCategory> categories = new HashSet<>();

    /**
     * Relacja z serwisami przez tabelę łączącą BikeServicePricelistItem
     */
    @OneToMany(mappedBy = "pricelistItem", fetch = FetchType.LAZY)
    private Set<BikeServicePricelistItem> bikeServicePricelistItems = new HashSet<>();

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

    // === METODY POMOCNICZE ===

    /**
     * Dodaje kategorię do pozycji
     */
    public void addCategory(PricelistCategory category) {
        categories.add(category);
        category.getPricelistItems().add(this);
    }

    /**
     * Usuwa kategorię z pozycji
     */
    public void removeCategory(PricelistCategory category) {
        categories.remove(category);
        category.getPricelistItems().remove(this);
    }

    /**
     * Sprawdza czy pozycja ma bazową cenę
     */
    public boolean hasBasePrice() {
        return basePrice != null && !basePrice.trim().isEmpty();
    }

    /**
     * Sprawdza czy pozycja ma opis
     */
    public boolean hasDescription() {
        return description != null && !description.trim().isEmpty();
    }

    /**
     * Pobiera wszystkie serwisy które oferują tę pozycję
     */
    public Set<BikeServiceRegistered> getBikeServices() {
        return bikeServicePricelistItems.stream()
                .map(BikeServicePricelistItem::getBikeService)
                .collect(Collectors.toSet());
    }

    /**
     * Sprawdza czy pozycja jest używana przez jakiś serwis
     */
    public boolean isUsedByServices() {
        return !bikeServicePricelistItems.isEmpty();
    }

    /**
     * Sprawdza czy pozycja należy do jakiejś kategorii
     */
    public boolean hasCategories() {
        return !categories.isEmpty();
    }
}