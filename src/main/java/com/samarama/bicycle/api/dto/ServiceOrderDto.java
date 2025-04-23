package com.samarama.bicycle.api.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record ServiceOrderDto(
        @NotNull List<Long> bicycleIds,

        // Może być albo id pakietu albo kod pakietu (dla kompatybilności)
        Long servicePackageId,

        // Dla wstecznej kompatybilności
        String servicePackageCode,

        @NotNull @Future LocalDate pickupDate,

        @NotBlank String pickupAddress,

        Double pickupLatitude,

        Double pickupLongitude,

        String additionalNotes
) {
    /**
     * Sprawdza, czy pakiet serwisowy został określony (albo przez ID, albo przez kod)
     */
    public boolean hasServicePackage() {
        return servicePackageId != null || servicePackageCode != null;
    }
}