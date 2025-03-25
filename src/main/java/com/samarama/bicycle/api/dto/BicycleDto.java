package com.samarama.bicycle.api.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public record BicycleDto(
        @NotBlank String frameNumber,
        @NotBlank String brand,
        String model,
        String type,
        LocalDate productionDate
) {
}
