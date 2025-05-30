package com.samarama.bicycle.api.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

/**
 * DTO dla zamówienia transportu rowerów
 */
public record TransportOrderDto(
        @NotNull List<Long> bicycleIds,

        @NotNull @Future LocalDate pickupDate,

        @NotBlank String pickupAddress,

        Double pickupLatitude,

        Double pickupLongitude,

        @NotBlank String deliveryAddress,

        Double deliveryLatitude,

        Double deliveryLongitude,

        String additionalNotes
) {
    /**
     * Sprawdza, czy zamówienie ma wymagane dane
     */
    public boolean isValid() {
        return bicycleIds != null && !bicycleIds.isEmpty() &&
                pickupDate != null &&
                pickupAddress != null && !pickupAddress.trim().isEmpty() &&
                deliveryAddress != null && !deliveryAddress.trim().isEmpty();
    }

    /**
     * Sprawdza, czy ma kompletne współrzędne geograficzne
     */
    public boolean hasCompleteCoordinates() {
        return pickupLatitude != null && pickupLongitude != null &&
                deliveryLatitude != null && deliveryLongitude != null;
    }
}