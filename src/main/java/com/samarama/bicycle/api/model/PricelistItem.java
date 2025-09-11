package com.samarama.bicycle.api.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * Pozycja cennika - pojedyncza usługa z ceną
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "pricelist_items")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@ToString(exclude = "category") // Zapobiega cyklicznym referencjom w toString
public class PricelistItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /**
     * Kategoria do której należy pozycja
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    @JsonBackReference
    private PricelistCategory category;

    /**
     * Nazwa usługi (np. "Wymiana łańcucha")
     */
    @NotBlank
    @Size(max = 200)
    @Column(name = "service_name", nullable = false)
    private String serviceName;

    /**
     * Cena usługi (np. "50zł", "od 30zł", "50-80zł")
     */
    @NotBlank
    @Size(max = 100)
    @Column(name = "price", nullable = false)
    private String price;

    /**
     * Dodatkowy opis usługi
     */
    @Size(max = 1000)
    @Column(name = "description")
    private String description;

    /**
     * Kolejność wyświetlania w kategorii
     */
    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

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
     * Zwraca nazwę kategorii
     */
    public String getCategoryName() {
        return category != null ? category.getName() : null;
    }

    /**
     * Zwraca ID serwisu przez kategorię
     */
    public Long getBikeServiceId() {
        return category != null ? category.getBikeServiceId() : null;
    }

    /**
     * Sprawdza czy pozycja ma opis
     */
    public boolean hasDescription() {
        return description != null && !description.trim().isEmpty();
    }
}
