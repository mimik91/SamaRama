package com.samarama.bicycle.api.dto;

import com.samarama.bicycle.api.model.ServiceOrder;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record ServiceOrderDto(
        @NotNull Long bicycleId,
        @NotNull ServiceOrder.ServicePackage servicePackage,
        @NotNull @Future LocalDate pickupDate,
        @NotNull String pickupAddress,
        Double pickupLatitude,
        Double pickupLongitude,
        String additionalNotes
) {
}