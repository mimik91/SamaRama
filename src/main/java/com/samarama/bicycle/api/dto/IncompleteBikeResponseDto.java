package com.samarama.bicycle.api.dto;

import com.samarama.bicycle.api.model.IncompleteBike;
import java.time.LocalDate;

public record IncompleteBikeResponseDto(
        Long id,
        String brand,
        String model,
        String type,
        String frameMaterial,
        LocalDate productionDate,
        boolean hasPhoto,
        boolean isComplete
) {
    public static IncompleteBikeResponseDto fromEntity(IncompleteBike entity) {
        return new IncompleteBikeResponseDto(
                entity.getId(),
                entity.getBrand(),
                entity.getModel(),
                entity.getType(),
                entity.getFrameMaterial(),
                entity.getProductionDate(),
                entity.hasPhoto(),
                false // Zawsze niekompletny
        );
    }

    // Metoda do tworzenia z encji Bicycle z flagą isComplete=true
    public static IncompleteBikeResponseDto fromBicycleEntity(com.samarama.bicycle.api.model.Bicycle entity) {
        return new IncompleteBikeResponseDto(
                entity.getId(),
                entity.getBrand(),
                entity.getModel(),
                entity.getType(),
                entity.getFrameMaterial(),
                entity.getProductionDate(),
                entity.hasPhoto(),
                true // Zawsze kompletny
        );
    }
}