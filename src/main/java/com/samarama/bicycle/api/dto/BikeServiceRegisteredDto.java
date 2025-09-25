package com.samarama.bicycle.api.dto;

import com.samarama.bicycle.api.model.BikeServiceRegistered;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO dla zarejestrowanego serwisu rowerowego
 * Zawiera wszystkie pola z BikeService oraz dodatkowe pola z BikeServiceRegistered
 * (z wyłączeniem pól związanych z cennikiem, godzinami otwarcia i pokryciem napraw)
 */
public record BikeServiceRegisteredDto(
        Long id,

        // === POLA Z BIKE_SERVICE ===
        @NotBlank
        @Size(max = 50)
        String name,

        @Email
        @Size(max = 100)
        String email,

        // Adres
        @Size(max = 255)
        String street,

        @Size(max = 20)
        String building,

        @Size(max = 20)
        String flat,

        @Size(max = 10)
        String postalCode,

        @Size(max = 100)
        String city,

        // Lokalizacja geograficzna
        Double latitude,
        Double longitude,

        // Kontakt
        @Size(max = 15)
        String phoneNumber,

        // Transport
        @PositiveOrZero
        BigDecimal transportCost,
        boolean transportAvailable,

        // Metadane
        LocalDateTime createdAt,
        LocalDateTime updatedAt,

        // === POLA Z BIKE_SERVICE_REGISTERED ===
        @Size(max = 100)
        String suffix,

        @Size(max = 100)
        String contactPerson,

        @Size(max = 255)
        String website,

        @Size(max = 1500)
        String description
) {

    /**
     * Konwertuje encję BikeServiceRegistered na DTO
     */
    public static BikeServiceRegisteredDto fromEntity(BikeServiceRegistered entity) {
        return new BikeServiceRegisteredDto(
                entity.getId(),

                // Pola z BikeService
                entity.getName(),
                entity.getEmail(),
                entity.getStreet(),
                entity.getBuilding(),
                entity.getFlat(),
                entity.getPostalCode(),
                entity.getCity(),
                entity.getLatitude(),
                entity.getLongitude(),
                entity.getPhoneNumber(),
                entity.getTransportCost(),
                entity.isTransportAvailable(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),

                // Pola z BikeServiceRegistered
                entity.getSuffix(),
                entity.getContactPerson(),
                entity.getWebsite(),
                entity.getDescription()
        );
    }

    /**
     * Konwertuje DTO na nową encję BikeServiceRegistered
     */
    public BikeServiceRegistered toEntity() {
        BikeServiceRegistered entity = new BikeServiceRegistered();
        updateEntity(entity);
        return entity;
    }

    /**
     * Aktualizuje istniejącą encję BikeServiceRegistered danymi z DTO
     */
    public void updateEntity(BikeServiceRegistered entity) {
        // Pola z BikeService
        entity.setName(this.name);
        entity.setEmail(this.email);
        entity.setStreet(this.street);
        entity.setBuilding(this.building);
        entity.setFlat(this.flat);
        entity.setPostalCode(this.postalCode);
        entity.setCity(this.city);
        entity.setLatitude(this.latitude);
        entity.setLongitude(this.longitude);
        entity.setPhoneNumber(this.phoneNumber);
        entity.setTransportCost(this.transportCost);
        entity.setTransportAvailable(this.transportAvailable);

        // Pola z BikeServiceRegistered
        entity.setSuffix(this.suffix);
        entity.setContactPerson(this.contactPerson);
        entity.setWebsite(this.website);
        entity.setDescription(this.description);
    }

    /**
     * Pobiera pełny adres jako string
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