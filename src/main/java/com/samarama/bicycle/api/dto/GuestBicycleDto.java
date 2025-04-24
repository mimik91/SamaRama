package com.samarama.bicycle.api.dto;

// Zagnieżdżona klasa dla rowerów
public record GuestBicycleDto(
        String brand,
        String model,
        String additionalInfo
) {}
