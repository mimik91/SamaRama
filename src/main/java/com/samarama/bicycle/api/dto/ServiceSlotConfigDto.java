package com.samarama.bicycle.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

/**
 * DTO dla konfiguracji slotów serwisowych
 */
public record ServiceSlotConfigDto(
        Long id,

        @NotNull
        LocalDate startDate,

        LocalDate endDate,

        @NotNull
        @Positive
        Integer maxBikesPerDay,

        @Positive
        Integer maxBikesPerOrder
) {
    /**
     * Tworzy DTO z modelu
     */
    public static ServiceSlotConfigDto fromEntity(com.samarama.bicycle.api.model.ServiceSlotConfig entity) {
        return new ServiceSlotConfigDto(
                entity.getId(),
                entity.getStartDate(),
                entity.getEndDate(),
                entity.getMaxBikesPerDay(),
                entity.getMaxBikesPerOrder()
        );
    }

    /**
     * Zwraca efektywną wartość maxBikesPerOrder
     */
    public int getEffectiveMaxBikesPerOrder() {
        return maxBikesPerOrder != null ? maxBikesPerOrder : maxBikesPerDay;
    }
}