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

/**
 * Kategoria cennika serwisu rowerowego - może być współdzielona między serwisami
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "pricelist_categories")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@ToString(exclude = {"bikeServices", "pricelistItems"})
public class PricelistCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /**
     * Nazwa kategorii (np. "Podstawowe naprawy", "Serwis kół")
     */
    @NotBlank
    @Size(max = 100)
    @Column(name = "name", nullable = false, unique = true)
    private String name;

    /**
     * Opis kategorii
     */
    @Size(max = 500)
    @Column(name = "description")
    private String description;

    /**
     * Serwisy które używają tej kategorii
     */
    @ManyToMany(mappedBy = "pricelistCategories", fetch = FetchType.LAZY)
    private Set<BikeServiceRegistered> bikeServices = new HashSet<>();

    /**
     * Pozycje cennika w tej kategorii (dla wszystkich serwisów)
     */
    @ManyToMany(mappedBy = "categories", fetch = FetchType.LAZY)
    private Set<PricelistItem> pricelistItems = new HashSet<>();

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
     * Dodaje serwis do kategorii
     */
    public void addBikeService(BikeServiceRegistered bikeService) {
        bikeServices.add(bikeService);
        bikeService.getPricelistCategories().add(this);
    }

    /**
     * Usuwa serwis z kategorii
     */
    public void removeBikeService(BikeServiceRegistered bikeService) {
        bikeServices.remove(bikeService);
        bikeService.getPricelistCategories().remove(this);
    }

    /**
     * Sprawdza czy kategoria jest używana przez jakiś serwis
     */
    public boolean isUsedByServices() {
        return !bikeServices.isEmpty();
    }

    /**
     * Sprawdza czy kategoria ma jakieś pozycje
     */
    public boolean hasItems() {
        return !pricelistItems.isEmpty();
    }
}