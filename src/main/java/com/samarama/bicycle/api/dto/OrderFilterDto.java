package com.samarama.bicycle.api.dto;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * DTO do filtrowania zamówień
 */
public record OrderFilterDto(
        // === FILTROWANIE PO DACIE ===
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate pickupDateFrom,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate pickupDateTo,

        // === FILTROWANIE PO STATUSIE ===
        String status, // PENDING, CONFIRMED, PICKED_UP, IN_SERVICE, COMPLETED, DELIVERED, CANCELLED

        // === FILTROWANIE PO TYPIE (dla mixed queries) ===
        String orderType, // SERVICE, TRANSPORT, COMBINED

        // === WYSZUKIWANIE ===
        String searchTerm, // email lub telefon klienta

        // === FILTROWANIE TRANSPORTU ===
        String transportStatus, // PENDING, PICKED_UP, IN_TRANSIT, DELIVERED_TO_SERVICE, COMPLETED
        String transportType,   // TO_SERVICE_ONLY, SERVICE_WITH_TRANSPORT

        // === FILTROWANIE SERWISU ===
        String servicePackageCode,
        Long servicePackageId,

        // === SORTOWANIE ===
        String sortBy,     // orderDate, pickupDate, status, client
        String sortOrder   // ASC, DESC
) {
    /**
     * Sprawdza czy są ustawione filtry dat
     */
    public boolean hasDateFilter() {
        return pickupDateFrom != null || pickupDateTo != null;
    }

    /**
     * Sprawdza czy jest ustawiony filtr statusu
     */
    public boolean hasStatusFilter() {
        return status != null && !status.trim().isEmpty();
    }

    /**
     * Sprawdza czy jest ustawiony search term
     */
    public boolean hasSearchTerm() {
        return searchTerm != null && !searchTerm.trim().isEmpty();
    }

    /**
     * Sprawdza czy są ustawione filtry transportu
     */
    public boolean hasTransportFilters() {
        return transportStatus != null || transportType != null;
    }

    /**
     * Sprawdza czy są ustawione filtry serwisu
     */
    public boolean hasServiceFilters() {
        return servicePackageCode != null || servicePackageId != null;
    }

    /**
     * Zwraca domyślne sortowanie jeśli nie podano
     */
    public String getEffectiveSortBy() {
        return sortBy != null ? sortBy : "orderDate";
    }

    /**
     * Zwraca domyślny kierunek sortowania jeśli nie podano
     */
    public String getEffectiveSortOrder() {
        return sortOrder != null ? sortOrder : "DESC";
    }

    /**
     * Tworzy pusty filtr (bez ograniczeń)
     */
    public static OrderFilterDto empty() {
        return new OrderFilterDto(null, null, null, null, null, null, null, null, null, null, null);
    }

    /**
     * Tworzy filtr tylko z search term
     */
    public static OrderFilterDto withSearchTerm(String searchTerm) {
        return new OrderFilterDto(null, null, null, null, searchTerm, null, null, null, null, null, null);
    }

    /**
     * Tworzy filtr tylko ze statusem
     */
    public static OrderFilterDto withStatus(String status) {
        return new OrderFilterDto(null, null, status, null, null, null, null, null, null, null, null);
    }
}