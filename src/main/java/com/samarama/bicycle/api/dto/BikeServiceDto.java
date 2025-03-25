package com.samarama.bicycle.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.util.Map;

public record BikeServiceDto(
        @NotBlank String name,
        @NotBlank String address,
        String postalCode,
        String city,
        String phoneNumber,
        @Email String email,
        Map<String, String>openingHours,
        String description
) {
}
