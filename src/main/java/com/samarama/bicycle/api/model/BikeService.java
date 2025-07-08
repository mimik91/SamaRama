package com.samarama.bicycle.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Encja reprezentująca serwis rowerowy
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "bike_services")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class BikeService {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /**
     * Nazwa serwisu rowerowego
     */
    @NotBlank
    @Size(max = 100)
    @Column(name = "name", nullable = false)
    private String name;

    /**
     * Opis serwisu
     */
    @Size(max = 1000)
    @Column(name = "description")
    private String description;

    /**
     * Adres email kontaktowy
     */
    @Email
    @Size(max = 100)
    @Column(name = "email")
    private String email;

    /**
     * Strona internetowa
     */
    @Size(max = 255)
    @Column(name = "website")
    private String website;

    // === ADRES ===

    /**
     * Ulica
     */
    @Size(max = 255)
    @Column(name = "street")
    private String street;

    /**
     * Numer budynku
     */
    @Size(max = 20)
    @Column(name = "building")
    private String building;

    /**
     * Numer mieszkania/lokalu
     */
    @Size(max = 20)
    @Column(name = "flat")
    private String flat;

    /**
     * Kod pocztowy
     */
    @Size(max = 10)
    @Column(name = "postal_code")
    private String postalCode;

    /**
     * Miasto
     */
    @Size(max = 100)
    @Column(name = "city")
    private String city;

    // === LOKALIZACJA GEOGRAFICZNA ===

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

    // === KONTAKT ===

    /**
     * Numer telefonu główny
     */
    @Size(max = 15)
    @Column(name = "phone_number")
    private String phoneNumber;

    /**
     * Numer telefonu służbowy
     */
    @Size(max = 15)
    @Column(name = "business_phone")
    private String businessPhone;

    @PositiveOrZero
    @Column(name = "transport_cost", nullable = false)
    private BigDecimal transportCost = BigDecimal.ZERO;

    // === METADANE ===

    /**
     * Data utworzenia rekordu
     */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    /**
     * Data ostatniej aktualizacji
     */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

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

    /**
     * Zwraca pełny adres jako jeden string
     */
    public String getFullAddress() {
        StringBuilder address = new StringBuilder();

        if (street != null && !street.trim().isEmpty()) {
            address.append(street);
        }

        if (building != null && !building.trim().isEmpty()) {
            if (address.length() > 0) address.append(" ");
            address.append(building);
        }

        if (flat != null && !flat.trim().isEmpty()) {
            if (address.length() > 0) address.append("/");
            address.append(flat);
        }

        if (city != null && !city.trim().isEmpty()) {
            if (address.length() > 0) address.append(", ");
            address.append(city);
        }

        if (postalCode != null && !postalCode.trim().isEmpty()) {
            if (address.length() > 0) address.append(" ");
            address.append(postalCode);
        }

        return address.toString();
    }

    /**
     * Sprawdza czy serwis ma kompletne dane adresowe
     */
    public boolean hasCompleteAddress() {
        return street != null && !street.trim().isEmpty() &&
                city != null && !city.trim().isEmpty();
    }

    /**
     * Sprawdza czy serwis ma współrzędne geograficzne
     */
    public boolean hasCoordinates() {
        return latitude != null && longitude != null;
    }
}