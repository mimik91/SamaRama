package com.samarama.bicycle.api.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Ujednolicone DTO dla zamówień transportowych i serwisowych
 * Obsługuje zarówno użytkowników zalogowanych jak i gości
 */
public record ServiceOrTransportOrderDto(
        // === ROWERY ===
        List<Long> bicycleIds,              // dla zalogowanych użytkowników
        List<GuestBicycleDto> bicycles,     // dla gości lub nowych rowerów

        // === UŻYTKOWNIK/GOŚĆ ===
        Long userId,                        // dla zalogowanych użytkowników
        String clientEmail,                 // dla gości
        String clientPhone,                 // dla gości

        // === ADRES ODBIORU ===
        Long pickupAddressId,               // jeśli adres istnieje w systemie
        // Alternatywnie - bezpośrednie dane adresowe:
        String pickupStreet,
        String pickupBuildingNumber,
        String pickupApartmentNumber,
        String pickupCity,
        String pickupPostalCode,
        Double pickupLatitude,
        Double pickupLongitude,

        // === TRANSPORT ===
        @NotNull @Future
        LocalDate pickupDate,

        @PositiveOrZero
        BigDecimal transportPrice,          // domyślnie 0
        String transportNotes,
        Long targetServiceId,               // 1 = serwis własny, inne = zewnętrzne

        // === SERWIS (opcjonalne) ===
        Long servicePackageId,
        String serviceNotes,

        // === DODATKOWE ===
        String additionalNotes
) {
    // === METODY WALIDACYJNE ===

    /**
     * Sprawdza czy to zamówienie gościa
     */
    public boolean isGuestOrder() {
        return clientEmail != null && !clientEmail.trim().isEmpty();
    }

    /**
     * Sprawdza czy to zamówienie użytkownika
     */
    public boolean isUserOrder() {
        return userId != null;
    }

    /**
     * Sprawdza czy to zamówienie serwisowe (transport + serwis)
     */
    public boolean isServiceOrder() {
        return servicePackageId != null;
    }

    /**
     * Sprawdza czy to czysto transportowe (bez serwisu)
     */
    public boolean isTransportOnlyOrder() {
        return !isServiceOrder();
    }

    /**
     * Sprawdza czy używa adresu z systemu
     */
    public boolean usesExistingAddress() {
        return pickupAddressId != null;
    }

    /**
     * Sprawdza czy używa nowych danych adresowych
     */
    public boolean usesNewAddress() {
        return pickupStreet != null && !pickupStreet.trim().isEmpty() &&
                pickupBuildingNumber != null && !pickupBuildingNumber.trim().isEmpty() &&
                pickupCity != null && !pickupCity.trim().isEmpty();
    }

    /**
     * Sprawdza czy używa rowery z systemu
     */
    public boolean usesExistingBicycles() {
        return bicycleIds != null && !bicycleIds.isEmpty();
    }

    /**
     * Sprawdza czy używa nowe rowery
     */
    public boolean usesNewBicycles() {
        return bicycles != null && !bicycles.isEmpty();
    }

    // === WALIDACJA KOMPLETNOŚCI ===

    /**
     * Walidacja dla zalogowanego użytkownika
     */
    public boolean isValidForLoggedUser() {
        return userId != null &&
                pickupDate != null &&
                (usesExistingAddress() || usesNewAddress()) &&
                (usesExistingBicycles() || usesNewBicycles()) &&
                transportPrice != null &&
                targetServiceId != null;
    }

    /**
     * Walidacja dla gościa
     */
    public boolean isValidForGuest() {
        return clientEmail != null && !clientEmail.trim().isEmpty() &&
                clientPhone != null && !clientPhone.trim().isEmpty() &&
                pickupDate != null &&
                usesNewAddress() && // gość zawsze używa nowych adresów
                usesNewBicycles() && // gość zawsze używa nowych rowerów
                transportPrice != null &&
                targetServiceId != null;
    }

    /**
     * Walidacja dla zamówienia serwisowego
     */
    public boolean isValidServiceOrder() {
        return isServiceOrder() &&
                (targetServiceId == null || targetServiceId.equals(1L)); // serwis własny
    }

    /**
     * Walidacja dla zamówienia transportowego
     */
    public boolean isValidTransportOrder() {
        return isTransportOnlyOrder() &&
                targetServiceId != null &&
                !targetServiceId.equals(1L); // nie serwis własny
    }

    // === METODY POMOCNICZE ===

    /**
     * Zwraca całkowitą cenę zamówienia
     * Uwaga: servicePrice będzie pobrana z bazy na podstawie servicePackageId
     */
    public BigDecimal getTotalPrice() {
        // Tylko transport price - service price będzie dodana w serwisie
        return getTransportPrice();
    }

    /**
     * Zwraca cenę transportu (domyślnie 0)
     */
    public BigDecimal getTransportPrice() {
        return transportPrice != null ? transportPrice : BigDecimal.ZERO;
    }

    /**
     * Zwraca liczbę rowerów
     */
    public int getBicycleCount() {
        if (usesExistingBicycles()) {
            return bicycleIds.size();
        } else if (usesNewBicycles()) {
            return bicycles.size();
        }
        return 0;
    }

    /**
     * Sprawdza czy ma okno czasowe odbioru
     * Zwraca false - aktualnie stały czas 18-22
     */
    public boolean hasPickupTimeWindow() {
        return false; // Stały czas odbioru 18-22
    }

    /**
     * Zwraca stałe okno czasowe odbioru
     */
    public String getPickupTimeWindow() {
        return "18:00 - 22:00";
    }

    /**
     * Zwraca typ zamówienia jako string
     */
    public String getOrderType() {
        return isServiceOrder() ? "SERVICE" : "TRANSPORT";
    }

    /**
     * Tworzy pełny adres odbioru jako string
     */
    public String getPickupAddressString() {
        if (!usesNewAddress()) {
            return null;
        }

        StringBuilder address = new StringBuilder();
        address.append(pickupStreet).append(" ").append(pickupBuildingNumber);

        if (pickupApartmentNumber != null && !pickupApartmentNumber.trim().isEmpty()) {
            address.append("/").append(pickupApartmentNumber);
        }

        address.append(", ").append(pickupCity);

        if (pickupPostalCode != null && !pickupPostalCode.trim().isEmpty()) {
            address.append(" ").append(pickupPostalCode);
        }

        return address.toString();
    }

    // === FACTORY METHODS ===

    /**
     * Tworzy DTO dla zamówienia serwisowego użytkownika
     */
    public static ServiceOrTransportOrderDto createUserServiceOrder(
            Long userId, List<Long> bicycleIds, Long pickupAddressId,
            LocalDate pickupDate, Long servicePackageId, BigDecimal transportPrice) {
        return new ServiceOrTransportOrderDto(
                bicycleIds, null, userId, null, null,
                pickupAddressId, null, null, null, null, null, null, null,
                pickupDate, transportPrice, null, 1L, // serwis własny
                servicePackageId, null, null
        );
    }

    /**
     * Tworzy DTO dla zamówienia transportowego użytkownika
     */
    public static ServiceOrTransportOrderDto createUserTransportOrder(
            Long userId, List<Long> bicycleIds, Long pickupAddressId,
            LocalDate pickupDate, Long targetServiceId, BigDecimal transportPrice) {
        return new ServiceOrTransportOrderDto(
                bicycleIds, null, userId, null, null,
                pickupAddressId, null, null, null, null, null, null, null,
                pickupDate, transportPrice, null, targetServiceId,
                null, null, null
        );
    }

    /**
     * Tworzy DTO dla zamówienia gościa (zawsze serwisowe)
     */
    public static ServiceOrTransportOrderDto createGuestServiceOrder(
            String email, String phone, List<GuestBicycleDto> bicycles,
            String street, String buildingNumber, String apartmentNumber, String city,
            LocalDate pickupDate, Long servicePackageId, BigDecimal transportPrice) {
        return new ServiceOrTransportOrderDto(
                null, bicycles, null, email, phone,
                null, street, buildingNumber, apartmentNumber, city, null, null, null,
                pickupDate, transportPrice, null, 1L, // serwis własny
                servicePackageId, null, null
        );
    }
}