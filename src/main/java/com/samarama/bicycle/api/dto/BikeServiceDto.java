package com.samarama.bicycle.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Map;

public record BikeServiceDto(
        @NotBlank String name,
        @NotBlank String address,
        String postalCode,
        String city,
        String phoneNumber,
        @Email String email,
        Map<String, String>openingHours,
        String description,
        @NotBlank @Size(min = 6) String password
) {
}
