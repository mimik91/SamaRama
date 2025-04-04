package com.samarama.bicycle.api.dto;

public record CoordinateDto(
        Long serviceId,
        String name,
        Double latitude,
        Double longitude
) {
    public static CoordinateDto fromBikeService(com.samarama.bicycle.api.model.BikeService service) {
        return new CoordinateDto(
                service.getId(),
                service.getName(),
                service.getLatitude(),
                service.getLongitude()
        );
    }
}