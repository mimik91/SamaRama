package com.samarama.bicycle.api.dto;

import com.samarama.bicycle.api.model.Bicycle;
import java.time.LocalDate;

public record BicycleResponseDto(
        Long id,
        String frameNumber,
        String brand,
        String model,
        String type,
        String frameMaterial,
        LocalDate productionDate
) {
    public static BicycleResponseDto fromEntity(Bicycle entity) {
        return new BicycleResponseDto(
                entity.getId(),
                entity.getFrameNumber(),
                entity.getBrand(),
                entity.getModel(),
                entity.getType(),
                entity.getFrameMaterial(),
                entity.getProductionDate()
        );
    }
}