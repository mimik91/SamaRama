package com.samarama.bicycle.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

    /**
     * Kategoria cennika serwisu rowerowego
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Entity
    @Table(name = "pricelist_categories")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    @ToString(exclude = "items") // Zapobiega cyklicznym referencjom w toString
    public class PricelistCategory {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        @Column(name = "id")
        private Long id;

        /**
         * ID serwisu do którego należy kategoria
         */
        @NotNull
        @Column(name = "bike_service_id", nullable = false)
        private Long bikeServiceId;

        /**
         * Nazwa kategorii (np. "Podstawowe naprawy", "Serwis kół")
         */
        @NotBlank
        @Size(max = 100)
        @Column(name = "name", nullable = false)
        private String name;

        /**
         * Opis kategorii
         */
        @Size(max = 500)
        @Column(name = "description")
        private String description;

        /**
         * Kolejność wyświetlania kategorii
         */
        @Column(name = "display_order", nullable = false)
        private Integer displayOrder = 0;

        /**
         * Lista pozycji cennika w tej kategorii
         */
        @OneToMany(mappedBy = "category", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
        @OrderBy("displayOrder ASC, serviceName ASC")
        @JsonManagedReference
        private List<PricelistItem> items = new ArrayList<>();

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
         * Dodaje nową pozycję do kategorii
         */
        public PricelistItem addItem(String serviceName, String price) {
            PricelistItem item = new PricelistItem();
            item.setServiceName(serviceName);
            item.setPrice(price);
            item.setCategory(this);
            item.setDisplayOrder(items.size());
            items.add(item);
            return item;
        }

        /**
         * Usuwa pozycję z kategorii
         */
        public void removeItem(PricelistItem item) {
            items.remove(item);
            item.setCategory(null);
        }

        /**
         * Sprawdza czy kategoria ma jakieś pozycje
         */
        public boolean hasItems() {
            return !items.isEmpty();
        }

    }
