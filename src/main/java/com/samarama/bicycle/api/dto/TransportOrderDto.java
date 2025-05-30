package com.samarama.bicycle.api.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
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

        String additionalNotes,

        // === DODATKOWE POLA TRANSPORTOWE ===

        LocalTime pickupTimeFrom,

        LocalTime pickupTimeTo,

        String transportType, // TO_SERVICE_ONLY, SERVICE_WITH_TRANSPORT

        BigDecimal transportPrice,

        String transportNotes,

        Integer estimatedTime, // w minutach

        Long targetServiceId,

        // === POLA SERWISOWE (dla zamówień kombinowanych) ===

        Long servicePackageId,

        String servicePackageCode,

        BigDecimal servicePrice,

        String serviceNotes
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

    /**
     * Sprawdza czy ma ustawiony czas odbioru
     */
    public boolean hasPickupTimeWindow() {
        return pickupTimeFrom != null && pickupTimeTo != null;
    }

    /**
     * Sprawdza czy to zamówienie kombinowane (transport + serwis)
     */
    public boolean isCombinedOrder() {
        return "SERVICE_WITH_TRANSPORT".equals(transportType) &&
                (servicePackageId != null || servicePackageCode != null);
    }

    /**
     * Sprawdza czy ma przypisany pakiet serwisowy
     */
    public boolean hasServicePackage() {
        return servicePackageId != null ||
                (servicePackageCode != null && !servicePackageCode.trim().isEmpty());
    }

    /**
     * Zwraca całkowitą cenę (transport + serwis)
     */
    public BigDecimal getTotalPrice() {
        BigDecimal transport = transportPrice != null ? transportPrice : BigDecimal.ZERO;
        BigDecimal service = servicePrice != null ? servicePrice : BigDecimal.ZERO;
        return transport.add(service);
    }

    /**
     * Tworzy DTO dla prostego transportu (bez serwisu)
     */
    public static TransportOrderDto forTransportOnly(
            List<Long> bicycleIds,
            LocalDate pickupDate,
            String pickupAddress,
            String deliveryAddress,
            Long targetServiceId,
            BigDecimal transportPrice) {

        return new TransportOrderDto(
                bicycleIds, pickupDate, pickupAddress, null, null,
                deliveryAddress, null, null, null,
                null, null, "TO_SERVICE_ONLY", transportPrice, null, null,
                targetServiceId, null, null, null, null
        );
    }

    /**
     * Tworzy DTO dla zamówienia kombinowanego (transport + serwis)
     */
    public static TransportOrderDto forCombinedOrder(
            List<Long> bicycleIds,
            LocalDate pickupDate,
            String pickupAddress,
            String deliveryAddress,
            Long targetServiceId,
            BigDecimal transportPrice,
            Long servicePackageId,
            BigDecimal servicePrice) {

        return new TransportOrderDto(
                bicycleIds, pickupDate, pickupAddress, null, null,
                deliveryAddress, null, null, null,
                null, null, "SERVICE_WITH_TRANSPORT", transportPrice, null, null,
                targetServiceId, servicePackageId, null, servicePrice, null
        );
    }
}