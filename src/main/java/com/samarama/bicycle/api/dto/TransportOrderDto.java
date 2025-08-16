package com.samarama.bicycle.api.dto;

import com.samarama.bicycle.api.model.IncompleteBike;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * DTO dla zamówienia transportu (bazowego)
 * Może być czysto transportowe lub bazą dla ServiceOrderDto
 */
public record TransportOrderDto(
        // === ROWERY ===
        Long bicycleId, // dla zalogowanych użytkowników
        IncompleteBike bicycle, // dla gości

        // === TRANSPORT - ODBIÓR ===
        @NotNull @Future LocalDate pickupDate,
        @NotBlank String pickupAddress,
        Double pickupLatitude,
        Double pickupLongitude,
        LocalTime pickupTimeFrom,
        LocalTime pickupTimeTo,

        // === TRANSPORT - DOSTAWA ===
        String deliveryAddress, // opcjonalne - może być z targetService
        Double deliveryLatitude,
        Double deliveryLongitude,
        @NotNull Long targetServiceId, // ID serwisu docelowego

        // === CENY I SZCZEGÓŁY ===
        @NotNull BigDecimal transportPrice,
        Integer estimatedTime, // w minutach
        String transportNotes,
        String additionalNotes,

        // === DANE GOŚCI (gdy brak bicycleIds) ===
        String clientEmail,
        String clientPhone
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
        return bicycleId != null &&
                pickupDate != null &&
                pickupAddress != null && !pickupAddress.trim().isEmpty() &&
                targetServiceId != null &&
                transportPrice != null;
    }

    /**
     * Walidacja dla gościa
     */
    public boolean isValidForGuest() {
        return clientEmail != null && !clientEmail.trim().isEmpty() &&
                clientPhone != null && !clientPhone.trim().isEmpty() &&
                bicycle != null &&
                pickupDate != null &&
                pickupAddress != null && !pickupAddress.trim().isEmpty() &&
                targetServiceId != null &&
                transportPrice != null;
    }

    /**
     * Sprawdza czy ma okno czasowe odbioru
     */
    public boolean hasPickupTimeWindow() {
        return pickupTimeFrom != null && pickupTimeTo != null;
    }
}