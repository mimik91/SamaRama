package com.samarama.bicycle.api.dto;

import java.time.LocalDate;

/**
 * DTO reprezentujące dostępność slotów serwisowych na dany dzień
 */
public record ServiceSlotAvailabilityDto(
        LocalDate date,
        Integer maxBikesPerDay,
        Integer bookedBikes,
        Integer availableBikes,
        Boolean isAvailable,
        Integer maxBikesPerOrder
) {
    /**
     * Konstruktor statyczny tworzący DTO z podstawowych informacji
     */
    public static ServiceSlotAvailabilityDto of(
            LocalDate date,
            Integer maxBikesPerDay,
            Integer bookedBikes,
            Integer maxBikesPerOrder) {

        int available = maxBikesPerDay - bookedBikes;
        return new ServiceSlotAvailabilityDto(
                date,
                maxBikesPerDay,
                bookedBikes,
                available,
                available > 0,
                maxBikesPerOrder
        );
    }
}