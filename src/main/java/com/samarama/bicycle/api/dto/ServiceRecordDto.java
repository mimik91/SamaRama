package com.samarama.bicycle.api.dto;

import com.samarama.bicycle.api.model.BikeService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ServiceRecordDto(
        @NotNull Long bicycleId,
        @NotBlank String name,
        @NotBlank String description,
        @NotNull LocalDate serviceDate,
        BigDecimal price
) {
}
