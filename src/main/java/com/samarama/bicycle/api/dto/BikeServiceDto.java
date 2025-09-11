package com.samarama.bicycle.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO dla serwisu rowerowego - używane do transferu danych między kontrolerem a klientem
 */
public record BikeServiceDto(
        Long id,

        @NotBlank
        @Size(min = 2, max = 100)
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

        // Koszt transportu
        @DecimalMin(value = "0.0", message = "Koszt transportu nie może być ujemny")
        BigDecimal transportCost,

        // Metadane (tylko do odczytu)
        LocalDateTime createdAt,
        LocalDateTime updatedAt,

        boolean transportAvailable
) {
        /**
         * Tworzy DTO z encji BikeService
         */
        public static BikeServiceDto fromEntity(com.samarama.bicycle.api.model.BikeService entity) {
                return new BikeServiceDto(
                        entity.getId(),
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
                        entity.getCreatedAt(),
                        entity.getUpdatedAt(),
                        entity.isTransportAvailable()
                );
        }

        /**
         * Tworzy encję BikeService z DTO (bez ID i metadanych)
         */
        public com.samarama.bicycle.api.model.BikeService toEntity() {
                com.samarama.bicycle.api.model.BikeService entity = new com.samarama.bicycle.api.model.BikeService();
                entity.setName(this.name);
                entity.setEmail(this.email.isBlank() ? null : this.email);
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
                return entity;
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

        /**
         * Sprawdza czy serwis ma ustawiony koszt transportu
         */
        public boolean hasTransportCost() {
                return transportCost != null && transportCost.compareTo(BigDecimal.ZERO) >= 0;
        }

        /**
         * Zwraca sformatowany koszt transportu jako string
         */
        public String getFormattedTransportCost() {
                if (transportCost == null) {
                        return "Brak danych";
                }
                return String.format("%.2f zł", transportCost);
        }
}