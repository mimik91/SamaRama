package com.samarama.bicycle.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Encja reprezentująca adres użytkownika
 * Może być używana dla adresów odbioru/dostawy w zamówieniach
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "addresses")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Address {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /**
     * Ulica
     */
    @NotBlank
    @Size(max = 255)
    @Column(name = "street", nullable = false)
    private String street;

    /**
     * Numer budynku
     */
    @NotBlank
    @Size(max = 20)
    @Column(name = "building_number", nullable = false)
    private String buildingNumber;

    /**
     * Numer mieszkania/lokalu (opcjonalne)
     */
    @Size(max = 20)
    @Column(name = "apartment_number")
    private String apartmentNumber;

    /**
     * Miasto
     */
    @NotBlank
    @Size(max = 100)
    @Column(name = "city", nullable = false)
    private String city;

    /**
     * Kod pocztowy (opcjonalne)
     */
    @Size(max = 10)
    @Column(name = "postal_code")
    private String postalCode;

    /**
     * Nazwa adresu (np. "Dom", "Praca", "Główny adres")
     */
    @Size(max = 100)
    @Column(name = "name")
    private String name;

    /**
     * ID użytkownika - właściciela adresu
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * Szerokość geograficzna
     */
    @Column(name = "latitude")
    private Double latitude;

    /**
     * Długość geograficzna
     */
    @Column(name = "longitude")
    private Double longitude;

    /**
     * Notatki transportowe specyficzne dla tego adresu
     */
    @Size(max = 500)
    @Column(name = "transport_notes")
    private String transportNotes;

    /**
     * Czy adres jest aktywny (nie usunięty)
     */
    @Column(name = "active", nullable = false)
    private boolean active = true;

    /**
     * Data utworzenia
     */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    /**
     * Data ostatniej aktualizacji
     */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    /**
     * Zwraca pełny adres jako jeden string
     */
    public String getFullAddress() {
        StringBuilder address = new StringBuilder();

        address.append(street).append(" ").append(buildingNumber);

        if (apartmentNumber != null && !apartmentNumber.trim().isEmpty()) {
            address.append("/").append(apartmentNumber);
        }

        address.append(", ").append(city);

        if (postalCode != null && !postalCode.trim().isEmpty()) {
            address.append(" ").append(postalCode);
        }

        return address.toString();
    }

    /**
     * Sprawdza czy adres ma współrzędne geograficzne
     */
    public boolean hasCoordinates() {
        return latitude != null && longitude != null;
    }

    /**
     * Automatyczna aktualizacja daty modyfikacji przed zapisem
     */
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Ustawienie daty utworzenia przed pierwszym zapisem
     */
    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        this.updatedAt = LocalDateTime.now();
    }

    // === KONSTRUKTORY POMOCNICZE ===

    public Address(String street, String buildingNumber, String city, Long userId) {
        this.street = street;
        this.buildingNumber = buildingNumber;
        this.city = city;
        this.userId = userId;
        this.active = true;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public Address(String street, String buildingNumber, String apartmentNumber,
                   String city, String postalCode, Long userId) {
        this(street, buildingNumber, city, userId);
        this.apartmentNumber = apartmentNumber;
        this.postalCode = postalCode;
    }
}