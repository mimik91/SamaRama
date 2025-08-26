package com.samarama.bicycle.api.dto;

/**
 * DTO dla pinów serwisów rowerowych na mapie - zawiera tylko ID i współrzędne geograficzne
 */
public record BikeServicePinDto(
        Long id,
        String ServiceName,
        Double latitude,
        Double longitude
) {
    /**
     * Tworzy DTO z encji BikeService - tylko dla serwisów z współrzędnymi
     */
    public static BikeServicePinDto fromEntity(com.samarama.bicycle.api.model.BikeService entity) {
        return new BikeServicePinDto(
                entity.getId(),
                entity.getName(),
                entity.getLatitude(),
                entity.getLongitude()
        );
    }

    /**
     * Sprawdza czy pin ma poprawne współrzędne
     */
    public boolean hasValidCoordinates() {
        return latitude != null && longitude != null &&
                latitude >= -90 && latitude <= 90 &&
                longitude >= -180 && longitude <= 180;
    }
}