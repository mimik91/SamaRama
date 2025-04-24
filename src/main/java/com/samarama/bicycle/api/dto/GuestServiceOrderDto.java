package com.samarama.bicycle.api.dto;

import java.time.LocalDate;
import java.util.List;

public record GuestServiceOrderDto(
        // Dane użytkownika
        String email,
        String phone,

        // Dane adresowe
        String address,
        String city,
        String notes,

        // Dane rowerów
        List<GuestBicycleDto> bicycles,

        // Dane zamówienia
        Long servicePackageId,
        LocalDate pickupDate
) {}

