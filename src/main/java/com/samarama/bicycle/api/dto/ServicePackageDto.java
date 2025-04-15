package com.samarama.bicycle.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO dla encji ServicePackage
 * Używane do transferu danych między kontrolerem a klientem
 */
public record ServicePackageDto(
        Long id,

        @NotBlank
        @Size(min = 2, max = 20)
        String code,

        @NotBlank
        @Size(min = 2, max = 100)
        String name,

        @Size(max = 1000)
        String description,

        @NotNull
        @PositiveOrZero
        BigDecimal price,

        Boolean active,

        Integer displayOrder,

        List<String> features
) {
    // Statyczna metoda do tworzenia obiektu DTO z encji
    public static ServicePackageDto fromEntity(com.samarama.bicycle.api.model.ServicePackage entity) {
        return new ServicePackageDto(
                entity.getId(),
                entity.getCode(),
                entity.getName(),
                entity.getDescription(),
                entity.getPrice(),
                entity.isActive(),
                entity.getDisplayOrder(),
                entity.getFeatures()
        );
    }
}