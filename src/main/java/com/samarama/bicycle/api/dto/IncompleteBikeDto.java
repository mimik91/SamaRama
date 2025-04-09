package com.samarama.bicycle.api.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public record IncompleteBikeDto(
        @NotBlank String brand,
        String model,
        String type,
        String frameMaterial,
        LocalDate productionDate
) {
}