package com.samarama.bicycle.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * DTO dla adresu
 */
public record AddressDto(
        Long id,

        @NotBlank
        @Size(max = 255)
        String street,

        @NotBlank
        @Size(max = 20)
        String buildingNumber,

        @Size(max = 20)
        String apartmentNumber,

        @NotBlank
        @Size(max = 100)
        String city,

        @Size(max = 10)
        String postalCode,

        @Size(max = 100)
        String name,

        Long userId,

        Double latitude,
        Double longitude,

        @Size(max = 500)
        String transportNotes,

        Boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    /**
     * Tworzy DTO z encji Address
     */
    public static AddressDto fromEntity(com.samarama.bicycle.api.model.Address entity) {
        return new AddressDto(
                entity.getId(),
                entity.getStreet(),
                entity.getBuildingNumber(),
                entity.getApartmentNumber(),
                entity.getCity(),
                entity.getPostalCode(),
                entity.getName(),
                entity.getUserId(),
                entity.getLatitude(),
                entity.getLongitude(),
                entity.getTransportNotes(),
                entity.isActive(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    /**
     * Tworzy encję Address z DTO
     */
    public com.samarama.bicycle.api.model.Address toEntity() {
        com.samarama.bicycle.api.model.Address entity = new com.samarama.bicycle.api.model.Address();
        entity.setStreet(this.street);
        entity.setBuildingNumber(this.buildingNumber);
        entity.setApartmentNumber(this.apartmentNumber);
        entity.setCity(this.city);
        entity.setPostalCode(this.postalCode);
        entity.setName(this.name);
        entity.setUserId(this.userId);
        entity.setLatitude(this.latitude);
        entity.setLongitude(this.longitude);
        entity.setTransportNotes(this.transportNotes);
        entity.setActive(this.active != null ? this.active : true);
        return entity;
    }

    /**
     * Zwraca pełny adres jako string
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
     * Tworzy DTO dla nowego adresu (bez ID i metadanych)
     */
    public static AddressDto createNew(String street, String buildingNumber, String apartmentNumber,
                                       String city, String postalCode, String name, Long userId) {
        return new AddressDto(
                null, street, buildingNumber, apartmentNumber, city, postalCode,
                name, userId, null, null, null, true, null, null
        );
    }
}