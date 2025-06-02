package com.samarama.bicycle.api.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * DTO dla zamówienia serwisowego (transport + serwis)
 * Dziedziczy wszystkie pola transportu i dodaje informacje o serwisie
 */
public record ServiceOrTransportOrderDto(
        List<Long> bicycleIds,
        List<GuestBicycleDto> bicycles,

        // === TRANSPORT - ODBIÓR (z TransportOrderDto) ===
        @NotNull @Future LocalDate pickupDate,
        @NotBlank String pickupAddress,
        Double pickupLatitude,
        Double pickupLongitude,
        LocalTime pickupTimeFrom,
        LocalTime pickupTimeTo,

        // === TRANSPORT - CENY (z TransportOrderDto) ===
        @NotNull BigDecimal transportPrice,
        Integer estimatedTime,
        String transportNotes,
        String additionalNotes,

        // === SERWIS - SPECYFICZNE DLA SERVICE ORDER ===
        Long servicePackageId,
        String servicePackageCode,
        BigDecimal servicePrice,
        String serviceNotes,

        // === DANE GOŚCI (z TransportOrderDto) ===
        String clientEmail,
        String clientPhone,
        String clientName,
        String city
) {
    /**
     * Sprawdza czy to zamówienie gościa
     */
    public boolean isGuestOrder() {
        return clientEmail != null && !clientEmail.trim().isEmpty();
    }

    /**
     * Walidacja dla zalogowanego użytkownika
     */
    public boolean isValidForLoggedUser() {
        return bicycleIds != null && !bicycleIds.isEmpty() &&
                pickupDate != null &&
                pickupAddress != null && !pickupAddress.trim().isEmpty() &&
                transportPrice != null &&
                hasValidServicePackage();
    }
    public boolean isValidForGuest() {
        return clientEmail != null && !clientEmail.trim().isEmpty() &&
                clientPhone != null && !clientPhone.trim().isEmpty() &&
                bicycles != null && !bicycles.isEmpty() &&
                pickupDate != null &&
                pickupAddress != null && !pickupAddress.trim().isEmpty() &&
                transportPrice != null &&
                hasValidServicePackage();
    }

    /**
     * Sprawdza czy ma pakiet serwisowy
     */
    public boolean hasValidServicePackage() {
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
     * Konwertuje do TransportOrderDto (bazowego)
     */
    public TransportOrderDto toTransportOrderDto() {
        return new TransportOrderDto(
                bicycleIds, bicycles, pickupDate, pickupAddress,
                pickupLatitude, pickupLongitude, pickupTimeFrom, pickupTimeTo,
                "SERWIS WŁASNY", // deliveryAddress dla service orders
                null, null, // delivery coordinates - będą ustawione automatycznie
                1L, // targetServiceId - serwis własny
                transportPrice, estimatedTime, transportNotes, additionalNotes,
                clientEmail, clientPhone, clientName, city
        );
    }
}